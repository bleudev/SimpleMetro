package com.bleudev.simplemetro.client.render

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.client.storage.MetroClientStorage
import com.bleudev.simplemetro.client.storage.MetroSchemeExtractor
import com.bleudev.simplemetro.config.SMetroInfo
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.TextAlignment
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.util.Util
import net.minecraft.world.level.Level
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Environment(EnvType.CLIENT)
internal object LinePanelRenderer {
    private val BG_TEXTURE = SMetro.resolve("panel_bg")

    private const val PANEL_WIDTH = 176
    private const val PANEL_HEIGHT = 78
    private const val PANEL_MARGIN = 7
    private const val MAX_ROUTE_STATIONS = 6

    private const val PANEL_IN_DURATION_MS = 230f
    private const val PANEL_MODE_TRANSITION_MS = 260L
    private const val ARRIVAL_NOTIFICATION_DURATION_MS = 3_600L
    private const val ARRIVAL_FADE_IN_MS = 280L
    private const val ARRIVAL_FADE_OUT_MS = 650L

    private const val COLOR_BACKGROUND = 0x101820
    private const val COLOR_MUTED = 0x9BACB8
    private const val COLOR_TEXT = 0xF3F7FA
    private const val COLOR_NODE = 0xDCE6EC
    private const val COLOR_BADGE = 0x1B2934

    private val dimension: Identifier
        get() = (Minecraft.getInstance().level?.dimension() ?: Level.OVERWORLD).identifier()

    private data class CacheKey(
        val dimension: Identifier,
        val schemeRevision: Int,
        val lineId: Int?,
        val previousVisibleStation: Int?,
        val currentStation: Int?,
        val nextVisibleStation: Int?,
    )

    private data class TransferState(
        val lineId: Int,
        val name: String,
        val color: Int,
    )

    private data class TableRenderState(
        val line: MetroSchemeExtractor.StationLineRenderState?,
        val currentStation: MetroSchemeExtractor.StationRenderState,
        val nextStation: MetroSchemeExtractor.StationRenderState?,
        val routeStations: List<MetroSchemeExtractor.StationRenderState>,
        val transfers: List<TransferState>,
        val travelling: Boolean,
    )

    private data class ActiveToast(
        val event: MetroClientStorage.StationEvent,
        val startedAtMs: Long,
        var fadeOutStartedAtMs: Long? = null,
    )

    private data class PanelTransition(
        val from: TableRenderState,
        val to: TableRenderState,
        val startedAtMs: Long,
    )

    private var cachedKey: CacheKey? = null
    private var tableRenderState: TableRenderState? = null
    private var panelVisibility = 0f
    private var lastFrameNanos = 0L
    private var activeToast: ActiveToast? = null
    private var panelTransition: PanelTransition? = null

    fun extractRenderState(extractor: GuiGraphicsExtractor) {
        val mc = Minecraft.getInstance()
        val storage = MetroClientStorage.getInstance()
        val dim = dimension
        val schemeExtractor = MetroSchemeExtractor.getInstance()
        val scheme = schemeExtractor.extractRenderState(dim)
        val revision = schemeExtractor.getRevision(dim)

        val key = CacheKey(
            dim,
            revision,
            storage.currentLine,
            storage.prevVisibleStation,
            storage.currentStation,
            storage.nextVisibleStation,
        )
        val nowMs = System.currentTimeMillis()
        val departureEvent = updateToast(storage, nowMs)
        if (key != cachedKey) {
            val previousState = tableRenderState
            val nextState = buildTableState(storage, SMetroInfo.getInstance(dim), scheme)
            panelTransition = if (
                departureEvent && activeToast == null && previousState != null && nextState != null
            ) {
                PanelTransition(previousState, nextState, nowMs)
            } else {
                null
            }
            tableRenderState = nextState
            cachedKey = key
        }

        val nowNanos = System.nanoTime()
        val deltaMs = if (lastFrameNanos == 0L) 16f
        else ((nowNanos - lastFrameNanos) / 1_000_000.0).toFloat().coerceIn(0f, 100f)
        lastFrameNanos = nowNanos

        val shouldShowPanel = mc.player != null && mc.level != null && tableRenderState != null
        panelVisibility = approach(panelVisibility, if (shouldShowPanel) 1f else 0f, deltaMs / PANEL_IN_DURATION_MS)

        val toast = activeToast
        val toastAlpha = toast?.let { getToastAlpha(it, nowMs) } ?: 0f

        if (panelVisibility > 0.001f && tableRenderState != null) {
            val alpha = easeOutCubic(panelVisibility)
            val x = (extractor.guiWidth() - PANEL_WIDTH - PANEL_MARGIN).coerceAtLeast(4)
            val y = PANEL_MARGIN
            val slide = ((1f - alpha) * (PANEL_WIDTH + 12)).roundToInt()
            if (panelTransition != null && toastAlpha <= 0.001f) {
                val progress = ((nowMs - panelTransition!!.startedAtMs).toFloat() / PANEL_MODE_TRANSITION_MS)
                    .coerceIn(0f, 1f)
                if (progress >= 1f) {
                    panelTransition = null
                    drawPanel(extractor, x + slide, y, tableRenderState!!, storage.stationProgress, alpha, alpha)
                } else {
                    drawPanel(extractor, x + slide, y, panelTransition!!.from, storage.stationProgress, alpha, alpha * (1f - progress))
                    drawPanel(extractor, x + slide, y, panelTransition!!.to, storage.stationProgress, alpha, alpha * progress)
                }
            } else {
                panelTransition = null
                drawPanel(
                    extractor,
                    x + slide,
                    y,
                    tableRenderState!!,
                    storage.stationProgress,
                    alpha,
                    alpha * (1f - toastAlpha),
                )
                toast?.let { active ->
                    drawArrivalNotificationPanel(extractor, x + slide, y, active.event, alpha * toastAlpha)
                }
            }
        }
    }

    private fun buildTableState(
        storage: MetroClientStorage,
        info: SMetroInfo,
        scheme: MetroSchemeExtractor.SchemeRenderState,
    ): TableRenderState? {
        val detectedCurrentId = storage.currentStation
        val travelling = detectedCurrentId == null &&
            storage.currentLine != null && storage.prevVisibleStation != null && storage.nextVisibleStation != null

        if (detectedCurrentId == null && !travelling) return null
        val currentId = if (travelling) storage.prevVisibleStation ?: return null
        else detectedCurrentId ?: return null
        val currentState = scheme.stations[currentId]?.takeUnless { it.status.invisible } ?: return null

        val lineId = storage.currentLine?.takeIf { id -> info.lines.any { it.id == id } }
            ?: currentState.lines.firstOrNull()?.id
        val sourceLine = lineId?.let { id -> info.lines.firstOrNull { it.id == id } }
        val line = sourceLine?.let { MetroSchemeExtractor.StationLineRenderState(it.id, it.name, it.color) }

        val nextId = storage.nextVisibleStation
        val nextState = nextId?.let(scheme.stations::get)?.takeUnless { it.status.invisible }

        var routeIds = sourceLine?.stations.orEmpty().filter { id ->
            scheme.stations[id]?.status?.invisible != true
        }
        val startIndex = routeIds.indexOf(currentId)
        if (startIndex >= 0 && nextId != null) {
            val targetIndex = routeIds.indexOf(nextId)
            if (targetIndex in 0..<startIndex) routeIds = routeIds.asReversed()
        }

        val orientedStart = routeIds.indexOf(currentId)
        val visibleRoute = if (line != null && orientedStart >= 0) {
            routeIds.drop(orientedStart).take(MAX_ROUTE_STATIONS).mapNotNull(scheme.stations::get)
        } else {
            listOf(currentState) + listOfNotNull(nextState)
        }

        val transfers = mutableListOf<TransferState>()
        val seenTransferLines = hashSetOf<Int>()
        fun addTransfer(transferLine: MetroSchemeExtractor.StationLineRenderState) {
            if (transferLine.id != lineId && transferLine.id >= 0 && seenTransferLines.add(transferLine.id)) {
                transfers.add(TransferState(transferLine.id, transferLine.name, transferLine.color))
            }
        }

        fun addLinkedStationLines(station: MetroSchemeExtractor.StationRenderState) {
            val linkedIds = LinkedHashSet<Int>()
            linkedIds.addAll(station.transfers)
            scheme.stations.values.asSequence()
                .filter { !it.status.invisible && station.id in it.transfers }
                .forEach { linkedIds.add(it.id) }
            linkedIds.forEach { transferStationId ->
                scheme.stations[transferStationId]
                    ?.takeUnless { it.status.invisible }
                    ?.lines
                    ?.forEach(::addTransfer)
            }
        }

        if (nextState != null) {
            nextState.lines.forEach(::addTransfer)
            addLinkedStationLines(nextState)
        } else {
            currentState.lines.forEach(::addTransfer)
            addLinkedStationLines(currentState)
        }

        return TableRenderState(
            line,
            currentState,
            nextState,
            visibleRoute,
            transfers,
            travelling,
        )
    }

    private fun drawPanel(
        extractor: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        state: TableRenderState,
        progress: Float?,
        alpha: Float,
        textAlpha: Float,
    ) {
        val accent = state.line?.color ?: COLOR_MUTED

        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, BG_TEXTURE, x - 10, y - 10, PANEL_WIDTH + 15, PANEL_HEIGHT + 20)
        extractor.fill(x + 1, y + 1, x + 4, y + PANEL_HEIGHT - 1, color(accent, alpha))

        val font = Minecraft.getInstance().font
        val lineLabel = state.line?.name ?: Component.translatable("smetro.hud.no_line").string
        extractor.scrollingTextLineLeft(Component.literal(lineLabel).withColor(COLOR_MUTED), alpha, x + 9, y + 4, PANEL_WIDTH - 42)

        val col1 = x + 9
        val col2 = x + (PANEL_WIDTH / 2) + 2
        val colWidth = (PANEL_WIDTH - 24) / 2
        extractor.text(
            font,
            Component.translatable(if (state.travelling) "smetro.hud.next" else "smetro.hud.here"),
            col1,
            y + 17,
            color(COLOR_MUTED, textAlpha),
        )
        if (!state.travelling) {
            extractor.text(
                font,
                Component.translatable("smetro.hud.next_short"),
                col2,
                y + 17,
                color(COLOR_MUTED, textAlpha),
            )
        }

        val currentStatus = state.currentStation.status.message?.string
        val nextStatus = state.nextStation?.let { it.status.message?.string }
        val currentColor = if (currentStatus != null) 0xFF9A8F else COLOR_TEXT
        val nextColor = if (nextStatus != null) 0xFFFFA2 else COLOR_TEXT

        if (state.travelling) {
            val nextName = state.nextStation?.name ?: Component.translatable(if (state.line == null) "smetro.hud.no_next_station" else "smetro.hud.terminal").string
            extractor.scrollingTextLineLeft(Component.literal(nextName).withColor(currentColor), textAlpha, col1, y + 26, colWidth * 2)
        }
        else {
            extractor.scrollingTextLineLeft(Component.literal(state.currentStation.name).withColor(currentColor), textAlpha, col1, y + 26, colWidth)
            val nextName = state.nextStation?.name ?: Component.translatable(if (state.line == null) "smetro.hud.no_next_station" else "smetro.hud.terminal").string
            extractor.scrollingTextLineLeft(Component.literal(nextName).withColor(nextColor), textAlpha, col2, y + 26, colWidth)
        }


        if (currentStatus != null && !state.travelling) {
            extractor.scrollingTextLineLeft(Component.literal(currentStatus).withColor(0xff8176), textAlpha, col1, y + 36, colWidth)
        }
        if (nextStatus != null) {
            extractor.scrollingTextLineLeft(Component.literal(nextStatus).withColor(0xffb36b), textAlpha, if (state.travelling) col1 else col2, y + 36, if (state.travelling) colWidth * 2 else colWidth)
        }

        if (state.line != null) {
            drawRouteDiagram(extractor, x, y + 51, state, progress, alpha)
        }
        if (state.transfers.isNotEmpty()) {
            drawTransfers(extractor, x + 9, y + 61, state.transfers, alpha)
        }
    }

    private fun drawArrivalNotificationPanel(
        extractor: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        event: MetroClientStorage.StationEvent,
        alpha: Float,
    ) {
        val font = Minecraft.getInstance().font
        val columnX = x + 9
        val columnWidth = PANEL_WIDTH - 24

        extractor.text(
            font,
            Component.translatable("smetro.hud.station"),
            columnX,
            y + 17,
            color(COLOR_MUTED, alpha),
        )
        extractor.scrollingTextLineLeft(
            Component.literal(event.stationName).withColor(color(COLOR_TEXT, alpha)),
            alpha,
            columnX,
            y + 26,
            columnWidth
        )
    }

    private fun drawRouteDiagram(
        extractor: GuiGraphicsExtractor,
        panelX: Int,
        y: Int,
        state: TableRenderState,
        progress: Float?,
        alpha: Float,
    ) {
        val stations = state.routeStations.take(MAX_ROUTE_STATIONS)
        if (stations.isEmpty()) return
        val startX = panelX + 10f
        val endX = panelX + PANEL_WIDTH - 10f
        val spacing = if (stations.size > 1) (endX - startX) / (stations.size - 1) else 0f
        val lineColor = state.line?.color ?: COLOR_NODE

        fill(extractor, startX.roundToInt(), y, endX.roundToInt() + 1, y + 1, COLOR_NODE, alpha * 0.35f)
        if (state.line != null) {
            fill(extractor, startX.roundToInt(), y, endX.roundToInt() + 1, y + 1, lineColor, alpha * 0.9f)
        }

        stations.forEachIndexed { index, station ->
            if (!station.status.invisible) {
                val cx = (startX + spacing * index).roundToInt()
                val nodeColor = if (!station.status.open) 0xFF8176 else lineColor
                fill(extractor, cx - 2, y - 2, cx + 3, y + 3, COLOR_BACKGROUND, alpha)
                fill(extractor, cx - 1, y - 1, cx + 2, y + 2, nodeColor, alpha)
            }
        }

        if (state.travelling && stations.size >= 2 && progress != null) {
            val markerX = (startX + spacing * progress.coerceIn(0f, 1f)).roundToInt()
            Minecraft.getInstance().player?.also { player ->
                PlayerFaceExtractor.extractRenderState(extractor, player.skin, markerX - 3, y - 3, 7)
            }
        } else {
            val markerX = startX.roundToInt()
            fill(extractor, markerX - 3, y - 3, markerX + 4, y + 4, COLOR_TEXT, alpha)
            fill(extractor, markerX - 1, y - 1, markerX + 2, y + 2, lineColor, alpha)
        }
    }

    private fun drawTransfers(
        extractor: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        transfers: List<TransferState>,
        alpha: Float,
    ) {
        val shownCount = minOf(transfers.size, 3)
        val badgeWidth = (PANEL_WIDTH - 20 - (shownCount - 1) * 3) / shownCount
        for (i in 0 until shownCount) {
            val transfer = transfers[i]
            val bx = x + i * (badgeWidth + 3)
            fill(extractor, bx, y, bx + badgeWidth, y + 10, COLOR_BADGE, alpha)
            fill(extractor, bx, y, bx + 2, y + 10, transfer.color, alpha)
            val label = if (transfers.size > 3 && i == shownCount - 1) "+${transfers.size - 2}" else transfer.name
            extractor.scrollingTextLineLeft(Component.literal(label), alpha, bx + 5, y + 1, badgeWidth - 8)
        }
    }

    private fun updateToast(storage: MetroClientStorage, nowMs: Long): Boolean {
        var departureEvent = false
        while (true) {
            val event = storage.pollStationEvent() ?: break
            when (event.type) {
                MetroClientStorage.StationEventType.ARRIVAL -> {
                    activeToast = ActiveToast(event, nowMs)
                }
                MetroClientStorage.StationEventType.DEPARTURE -> {
                    departureEvent = true
                    activeToast?.let { toast ->
                        if (toast.fadeOutStartedAtMs == null) {
                            toast.fadeOutStartedAtMs = maxOf(nowMs, toast.startedAtMs + ARRIVAL_FADE_IN_MS)
                        }
                    }
                }
            }
        }

        activeToast?.let { toast ->
            val fadeOutStartedAt = toast.fadeOutStartedAtMs
                ?: (toast.startedAtMs + ARRIVAL_NOTIFICATION_DURATION_MS - ARRIVAL_FADE_OUT_MS)
            if (nowMs >= fadeOutStartedAt + ARRIVAL_FADE_OUT_MS) activeToast = null
        }
        return departureEvent
    }

    private fun getToastAlpha(toast: ActiveToast, nowMs: Long): Float {
        val ageMs = (nowMs - toast.startedAtMs).coerceAtLeast(0L)
        val fadeIn = easeOutCubic((ageMs.toFloat() / ARRIVAL_FADE_IN_MS).coerceIn(0f, 1f))
        val fadeOutStartedAt = toast.fadeOutStartedAtMs
            ?: (toast.startedAtMs + ARRIVAL_NOTIFICATION_DURATION_MS - ARRIVAL_FADE_OUT_MS)
        val fadeOut = ((fadeOutStartedAt + ARRIVAL_FADE_OUT_MS - nowMs).toFloat() / ARRIVAL_FADE_OUT_MS)
            .coerceIn(0f, 1f)
        return minOf(fadeIn, fadeOut)
    }

    private fun fill(extractor: GuiGraphicsExtractor, x0: Int, y0: Int, x1: Int, y1: Int, rgb: Int, alpha: Float) {
        extractor.fill(x0, y0, x1, y1, color(rgb, alpha))
    }

    private fun ActiveTextCollector.tweakDefaultScrollingHelper(message: Component, centerX: Int, left: Int, right: Int, top: Int, bottom: Int, lineWidth: Int, lineHeight: Int, parameters: ActiveTextCollector.Parameters) {
        val textTop = (top + bottom - lineHeight) / 2 + 1
        val availableMessageWidth = right - left
        if (lineWidth > availableMessageWidth) {
            val maxPosition = lineWidth - availableMessageWidth
            val time = Util.getMillis().toDouble() / 1000.0
            val period = (maxPosition * 0.5F).coerceAtLeast(3.0F);
            val alpha = sin((Math.PI / 2) * cos((Math.PI * 2) * time / period)) / 2 + 0.5
            val pos = Mth.lerp(alpha, 0.0, maxPosition.toDouble())
            this.accept(TextAlignment.LEFT, left - pos.toInt(), textTop, parameters, message.visualOrderText);
        } else {
            val textX = Mth.clamp(centerX, left + lineWidth / 2, right - lineWidth / 2);
            this.accept(TextAlignment.CENTER, textX, textTop, parameters, message)
        }

    }
    private fun GuiGraphicsExtractor.scrollingTextLine(message: Component, alpha: Float, centerX: Int, x: Int, y: Int, width: Int) {
        if (alpha <= 0f || width == 0) return
        enableScissor(x, y, x + width, y + 10)
        val renderer = textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE)
        val params = renderer.defaultParameters().withOpacity(alpha)

        val lineWidth: Int = Minecraft.getInstance().font.width(message)
        val lineHeight = 9
        renderer.tweakDefaultScrollingHelper(message, centerX, x, x + width, y, y + 10, lineWidth, lineHeight, params)
        disableScissor()
    }
    private fun GuiGraphicsExtractor.scrollingTextLineLeft(message: Component, alpha: Float, x: Int, y: Int, width: Int) {
        scrollingTextLine(message, alpha, x, x, y, width)
    }

    private fun color(rgb: Int, alpha: Float): Int = ARGB.color(alpha.coerceIn(0f, 1f), rgb)

    private fun approach(value: Float, target: Float, amount: Float): Float {
        if (value < target) return (value + amount).coerceAtMost(target)
        if (value > target) return (value - amount).coerceAtLeast(target)
        return value
    }

    private fun easeOutCubic(value: Float): Float {
        val inverse = 1f - value.coerceIn(0f, 1f)
        return 1f - inverse * inverse * inverse
    }
}
