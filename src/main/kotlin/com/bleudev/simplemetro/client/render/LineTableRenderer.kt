package com.bleudev.simplemetro.client.render

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.client.storage.MetroClientStorage
import com.bleudev.simplemetro.client.storage.MetroSchemeExtractor
import com.bleudev.simplemetro.config.SMetroInfo
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.level.Level
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

internal object LineTableRenderer {
    private const val TOOLTIP_WIDTH = 125
    private const val TOOLTIP_HEIGHT = 100
    private const val TOOLTIP_OPAQUE = .6f
    private const val APPEAR_DURATION = 1000

    private val dim: Identifier get() = (Minecraft.getInstance().level?.dimension() ?: Level.OVERWORLD).identifier()
    private val schemeRenderState get() = MetroSchemeExtractor.getInstance().extractRenderState(dim)
    private var tableRenderState: TableRenderState? = null

    private var show: Boolean = false
    private var showTransTime: Int = 0
    private var prevTime: Long? = null

    fun setShow(show: Boolean) {
        this.show = show
    }

    fun tick(dt: Int) {
        if (this.show) this.showTransTime += dt
        else this.showTransTime -= dt
        this.showTransTime = this.showTransTime.coerceIn(0, APPEAR_DURATION)
    }

    fun rebuild(storage: MetroClientStorage, info: SMetroInfo) {
        val line = info.lines.firstOrNull { it.id == storage.currentLine }
        val station = storage.currentStation?.let { schemeRenderState.stations[it] }
        if (line == null || storage.prevStation == null) {
            tableRenderState = null
            return
        }

        var stations = line.stations
        var i = stations.indexOf(storage.prevStation!!)
        val j = storage.nextStation?.let { stations.indexOf(it) }
        if (j != null && j < i) {
            stations = stations.reversed()
            i = stations.size-1-i
        }
        stations = stations.drop(i)

        tableRenderState = TableRenderState(
            MetroSchemeExtractor.StationLineRenderState(line.id, line.name, line.color),
            station,
            stations.mapNotNull { schemeRenderState.stations[it] },
            storage.stationProgress,
        )
    }

    /**
     * Simple sin^2 interpolation
     * */
    private fun sin2Int(delta: Float): Float {
        return sin(PI.toFloat() * delta.coerceIn(0f, 1f) / 2).pow(2).coerceIn(0f, 1f)
    }

    fun extractRenderState(extractor: GuiGraphicsExtractor) {
        val storage = MetroClientStorage.getInstance()
        rebuild(storage, SMetroInfo.getInstance(dim))
        setShow(storage.currentLine != null)
        val currTime = System.currentTimeMillis()
        if (prevTime == null) prevTime = currTime
        tick((currTime-prevTime!!).toInt())
        prevTime = currTime

        extractor.pose().pushMatrix()
        extractor.pose().translate(-5f, -5f)
        extractor.pose().translate(0f, sin2Int(1f - (this.showTransTime).toFloat() / APPEAR_DURATION) * (-TOOLTIP_HEIGHT))
        extractContents(extractor)
        extractor.pose().popMatrix()
    }

    private fun extractContents(extractor: GuiGraphicsExtractor) {
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, SMetro.resolve("table_background"), 0, 0, TOOLTIP_WIDTH, TOOLTIP_HEIGHT, ARGB.color(TOOLTIP_OPAQUE, 0xffffff))

        tableRenderState?.let { state ->
            val col = ARGB.opaque(state.line.color)
            val component = Component.literal(state.line.name).withColor(col)
            val w = TOOLTIP_WIDTH
            val h = Minecraft.getInstance().font.lineHeight
            extractor.pose().pushMatrix()
            extractor.pose().translate(w.toFloat() / 2, 10f + h.toFloat() / 2)
            extractor.pose().scale(.5f)
            extractor.textRenderer().acceptScrollingWithDefaultCenter(
                component,
                -w / 2, w / 2,
                -h, h
            )
            extractor.pose().popMatrix()

            extractor.pose().pushMatrix()
            extractor.pose().translate(w.toFloat() / 2, 14f + h.toFloat())
            extractor.pose().scale(.75f)
            extractLine(extractor, state)
            extractor.pose().popMatrix()

            if (state.stations.size > 1) {
                extractor.pose().pushMatrix()
                extractor.pose().translate(13f, 20f + h.toFloat())
                extractStationInfo(extractor, state.stations[1])
                extractor.pose().popMatrix()
            }
        }
    }

    private fun extractLine(extractor: GuiGraphicsExtractor, state: TableRenderState) {
        val col = ARGB.opaque(state.line.color)
        val w = TOOLTIP_WIDTH
        extractor.fill(-w / 2, -1, w / 2, 1, col)

        state.progress?.also { progress ->
            val x = -w.toFloat() / 2 + (progress * (w - 12*(state.stations.size-2)))
            extractor.pose().pushMatrix()
            extractor.pose().translate(x, 0f)
            Minecraft.getInstance().player?.also { player ->
                PlayerFaceExtractor.extractRenderState(extractor, player.skin, -4, -4, 8)
            }
            extractor.pose().popMatrix()
        }

        extractor.fill(-w / 2 - 4, -4, -w / 2 + 4, 4, col)
        for (i in 0..state.stations.size-2) {
            extractor.fill(w / 2 - 4 - 12*i, -4, w / 2 + 4 - 12*i, 4, col)
        }

        println("DEBUG ${state.progress} ${state.stations.map { it.id to it.name }}")
    }

    private fun extractStationInfo(extractor: GuiGraphicsExtractor, station: MetroSchemeExtractor.StationRenderState) {
        val font = Minecraft.getInstance().font
        extractor.pose().pushMatrix()
        extractor.pose().scale(.5f)
        extractor.text(font, Component.literal("Next station").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), 0, 0, 0xffffffff.toInt())
        extractor.pose().popMatrix()

        extractor.text(font, Component.literal(station.name), 0, 7, 0xffffffff.toInt())
    }

    private data class TableRenderState(
        val line: MetroSchemeExtractor.StationLineRenderState,
        val current: MetroSchemeExtractor.StationRenderState?,
        val stations: List<MetroSchemeExtractor.StationRenderState>,
        val progress: Float?,
    )
}