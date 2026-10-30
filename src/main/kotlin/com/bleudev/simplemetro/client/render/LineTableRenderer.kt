package com.bleudev.simplemetro.client.render

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.client.storage.MetroClientStorage
import com.bleudev.simplemetro.client.storage.MetroSchemeExtractor
import com.bleudev.simplemetro.config.SMetroInfo
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.level.Level
import kotlin.math.roundToInt

/**
 * Compact, Moscow-metro-inspired route HUD.
 *
 * Only the route's static presentation is rebuilt when the dimension, line, station state or
 * scheme revision changes. Player progress and animation are sampled separately every frame.
 */
@Environment(EnvType.CLIENT)
internal object LineTableRenderer {
    private const val PANEL_WIDTH = 176
    private const val PANEL_HEIGHT = 78
    private const val PANEL_MARGIN = 7
    private const val MAX_ROUTE_STATIONS = 6

    private const val PANEL_IN_DURATION_MS = 230f
    private const val TITLE_DURATION_MS = 3_600L
    private const val TITLE_FADE_IN_MS = 280L
    private const val TITLE_FADE_OUT_MS = 650L
    private const val TITLE_SCALE = 3.0f

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
    )

    private var cachedKey: CacheKey? = null
    private var tableRenderState: TableRenderState? = null
    private var panelVisibility = 0f
    private var lastFrameNanos = 0L
    private var activeToast: ActiveToast? = null

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
        if (key != cachedKey) {
            tableRenderState = buildTableState(storage, SMetroInfo.getInstance(dim), scheme)
            cachedKey = key
        }

        val nowNanos = System.nanoTime()
        val deltaMs = if (lastFrameNanos == 0L) 16f
        else ((nowNanos - lastFrameNanos) / 1_000_000.0).toFloat().coerceIn(0f, 100f)
        lastFrameNanos = nowNanos

        val shouldShowPanel = mc.player != null && mc.level != null && tableRenderState != null
        panelVisibility = approach(panelVisibility, if (shouldShowPanel) 1f else 0f, deltaMs / PANEL_IN_DURATION_MS)

        updateToast(storage, System.currentTimeMillis())

        if (panelVisibility > 0.001f && tableRenderState != null) {
            val alpha = easeOutCubic(panelVisibility)
            val x = (extractor.guiWidth() - PANEL_WIDTH - PANEL_MARGIN).coerceAtLeast(4)
            val y = PANEL_MARGIN
            val slide = ((1f - alpha) * (PANEL_WIDTH + 12)).roundToInt()
            drawPanel(extractor, x + slide, y, tableRenderState!!, storage.stationProgress, alpha)
        }

        activeToast?.let { toast ->
            val now = System.currentTimeMillis()
            val age = (now - toast.startedAtMs).coerceAtLeast(0L)
            if (age < TITLE_DURATION_MS) {
                val fadeIn = (age.toFloat() / TITLE_FADE_IN_MS).coerceIn(0f, 1f)
                val fadeOut = ((TITLE_DURATION_MS - age).toFloat() / TITLE_FADE_OUT_MS).coerceIn(0f, 1f)
                val alpha = minOf(fadeIn, fadeOut)
                drawFullscreenNotification(extractor, toast.event, age, alpha)
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

        // Keep the line selected by the extractor. A station can belong to more than one line;
        // picking the first line here would make the panel jump to an unrelated branch.
        val lineId = storage.currentLine?.takeIf { id -> info.lines.any { it.id == id } }
            ?: currentState.lines.firstOrNull()?.id
        val sourceLine = lineId?.let { id -> info.lines.firstOrNull { it.id == id } }
        val line = sourceLine?.let { MetroSchemeExtractor.StationLineRenderState(it.id, it.name, it.color) }

        val nextId = storage.nextVisibleStation
        val nextState = nextId?.let(scheme.stations::get)?.takeUnless { it.status.invisible }

        // Invisible stations remain in the track geometry used by LineTableExtractor, but never
        // become visual stops on this diagram.
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
    ) {
        val accent = state.line?.color ?: COLOR_MUTED

        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, SMetro.resolve("table_background"), x - 10, y - 10, PANEL_WIDTH + 15, PANEL_HEIGHT + 20)
        fill(extractor, x + 1, y + 1, x + 4, y + PANEL_HEIGHT - 1, accent, alpha)

        val font = Minecraft.getInstance().font
        val lineLabel = state.line?.name ?: Component.translatable("smetro.hud.no_line").string
        extractor.textRenderer().acceptScrolling(Component.literal(lineLabel).withColor(color(COLOR_MUTED, alpha)), x + 9, x + 9, x + 9 + PANEL_WIDTH - 42, y + 4, y + 12)

        val col1 = x + 9
        val col2 = x + (PANEL_WIDTH / 2) + 2
        val colWidth = (PANEL_WIDTH - 24) / 2
        extractor.text(
            font,
            Component.translatable(if (state.travelling) "smetro.hud.next" else "smetro.hud.here"),
            col1,
            y + 17,
            color(COLOR_MUTED, alpha),
        )
        if (!state.travelling) {
            extractor.text(
                font,
                Component.translatable("smetro.hud.next_short"),
                col2,
                y + 17,
                color(COLOR_MUTED, alpha),
            )
        }

        val currentStatus = state.currentStation.status.message?.string
        val nextStatus = state.nextStation?.let { it.status.message?.string }
        val currentColor = if (currentStatus != null) 0xFF9A8F else COLOR_TEXT
        val nextColor = if (nextStatus != null) 0xFFFFA2 else COLOR_TEXT

        if (state.travelling) {
            val nextName = state.nextStation?.name ?: Component.translatable(if (state.line == null) "smetro.hud.no_next_station" else "smetro.hud.terminal").string
            extractor.textRenderer().acceptScrolling(Component.literal(nextName).withColor(color(currentColor, alpha)), col1, col1, col1 + colWidth * 2, y + 26, y + 36)
        }
        else {
            extractor.textRenderer().acceptScrolling(Component.literal(state.currentStation.name).withColor(color(currentColor, alpha)), col1, col1, col1 + colWidth, y + 26, y + 36)
            val nextName = state.nextStation?.name ?: Component.translatable(if (state.line == null) "smetro.hud.no_next_station" else "smetro.hud.terminal").string
            extractor.textRenderer().acceptScrolling(Component.literal(nextName).withColor(color(nextColor, alpha)), col2, col2, col2 + colWidth, y + 26, y + 36)
        }


        if (currentStatus != null && !state.travelling) {
            extractor.text(font, Component.literal(currentStatus), col1, y + 36, color(0xFFFF8176.toInt(), alpha))
        }
        if (nextStatus != null) {
            extractor.text(font, Component.literal(nextStatus), if (state.travelling) col1 else col2, y + 36, color(0xFFFFB36B.toInt(), alpha))
        }

        if (state.line != null) {
            drawRouteDiagram(extractor, x, y + 51, state, progress, alpha)
        }
        if (state.transfers.isNotEmpty()) {
            drawTransfers(extractor, x + 9, y + 61, state.transfers, alpha)
        }
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
            extractor.textRenderer().acceptScrolling(Component.literal(label), bx + 5, bx + 5, bx + 5 + badgeWidth - 8, y + 1, y + 10)
        }
    }

    private fun updateToast(storage: MetroClientStorage, nowMs: Long) {
        val active = activeToast
        if (active != null && nowMs - active.startedAtMs >= TITLE_DURATION_MS) {
            activeToast = null
        }
        if (activeToast == null) {
            storage.pollStationEvent()?.let { event ->
                activeToast = ActiveToast(event, nowMs)
            }
        }
    }

    private fun drawFullscreenNotification(
        extractor: GuiGraphicsExtractor,
        event: MetroClientStorage.StationEvent,
        ageMs: Long,
        alpha: Float,
    ) {
        val mc = Minecraft.getInstance()
        val font = mc.font
        val width = extractor.guiWidth()
        val height = extractor.guiHeight()
        val centerX = width / 2
        val centerY = height / 2
        val accent = when (event.type) {
            MetroClientStorage.StationEventType.ARRIVAL -> 0x53D7A1
            MetroClientStorage.StationEventType.DEPARTURE -> 0x58B8F5
        }
        val intro = easeOutCubic((ageMs.toFloat() / TITLE_FADE_IN_MS).coerceIn(0f, 1f))
        val scale = TITLE_SCALE * (0.88f + 0.12f * intro)
        val verticalOffset = ((1f - intro) * 8f).roundToInt()
        val titleWidth = (width / scale).toInt().coerceAtLeast(1)
        val subtitle = Component.translatable(
            when (event.type) {
                MetroClientStorage.StationEventType.ARRIVAL -> "smetro.hud.event.arrival"
                MetroClientStorage.StationEventType.DEPARTURE -> "smetro.hud.event.departure"
            },
        ).string

        extractor.pose().pushMatrix()
        extractor.pose().translate(centerX.toFloat(), (centerY - 22 + verticalOffset).toFloat())
        extractor.pose().scale(scale)
        extractor.textRenderer().acceptScrollingWithDefaultCenter(Component.literal(event.stationName).withColor(color(COLOR_TEXT, alpha)), -titleWidth / 2, titleWidth / 2, -4, 6)
        extractor.pose().popMatrix()

        extractor.pose().pushMatrix()
        extractor.pose().translate(centerX.toFloat(), (centerY + 22 + verticalOffset).toFloat())
        extractor.pose().scale(1.15f)
        extractor.centeredText(font, subtitle, 0, 0, color(accent, alpha))
        extractor.pose().popMatrix()
    }

    private fun fill(extractor: GuiGraphicsExtractor, x0: Int, y0: Int, x1: Int, y1: Int, rgb: Int, alpha: Float) {
        extractor.fill(x0, y0, x1, y1, color(rgb, alpha))
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
