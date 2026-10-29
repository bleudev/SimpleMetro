package com.bleudev.simplemetro.client.render

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.client.storage.MetroClientStorage
import com.bleudev.simplemetro.client.storage.MetroSchemeExtractor
import com.bleudev.simplemetro.config.SMetroInfo
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.level.Level
import org.joml.Vector2i
import org.joml.Vector2ic
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
        if (line == null) {
            tableRenderState = null
            return
        }

        tableRenderState = TableRenderState(
            MetroSchemeExtractor.StationLineRenderState(line.id, line.name, line.color),
            station,
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
            extractLine(extractor, state.line)
            extractor.pose().popMatrix()
        }
    }

    private fun extractLine(extractor: GuiGraphicsExtractor, line: MetroSchemeExtractor.StationLineRenderState) {
        val col = ARGB.opaque(line.color)
        val w = TOOLTIP_WIDTH
        extractor.fill(-w / 2, -1, w / 2, 1, col)
    }

    data class TableRenderState(
        val line: MetroSchemeExtractor.StationLineRenderState,
        val current: MetroSchemeExtractor.StationRenderState?
    )

    private object LineTableTooltipPositioner : ClientTooltipPositioner {
        override fun positionTooltip(
            screenWidth: Int,
            screenHeight: Int,
            x: Int,
            y: Int,
            tooltipWidth: Int,
            tooltipHeight: Int
        ): Vector2ic {
            return Vector2i(-tooltipWidth / 2, 0)
        }
    }
}