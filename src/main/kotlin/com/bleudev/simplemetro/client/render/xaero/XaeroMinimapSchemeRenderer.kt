package com.bleudev.simplemetro.client.render.xaero

import com.bleudev.simplemetro.client.render.MetroSchemeExtractor
import com.mojang.math.Axis
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.Font
import net.minecraft.world.phys.Vec3
import xaero.hud.minimap.element.render.MinimapElementGraphics

@Environment(EnvType.CLIENT)
internal object XaeroMinimapSchemeRenderer : XaeroCommonSchemeRenderer() {
    private var zoom: Double = 1.0

    @JvmStatic
    fun render(
        graphics: MinimapElementGraphics,
        renderPos: Vec3,
        ps: Double,
        pc: Double,
        zoom: Double,
        minimapScale: Float,
        backgroundCoordinateScale: Double,
        playerCoordinateScale: Double,
    ) {
        this.zoom = zoom
        val coordinateMultiplier = if (backgroundCoordinateScale == 0.0) 1.0 else playerCoordinateScale / backgroundCoordinateScale
        extractRenderState(object : MinimapGraphicsExtractor(graphics) {
            override fun mapScale(): Float = minimapScale
            override fun mousePos(): DrawPosition = DrawPosition(0f, 0f)
            override fun drawPos(x: Int, z: Int): DrawPosition {
                val offX = x * coordinateMultiplier - renderPos.x
                val offZ = z * coordinateMultiplier - renderPos.z
                val x = (ps * offX - pc * offZ) * zoom
                val y = (pc * offX + ps * offZ) * zoom
                return DrawPosition(x.toFloat(), y.toFloat())
            }
        })
    }

    override fun drawStationText(
        extractor: CommonGraphicsExtractor,
        station: MetroSchemeExtractor.StationRenderState,
        alpha: Float
    ) {
        val alpha = (this.zoom - 2.0).coerceIn(0.0, 1.0).toFloat()
        super.drawStationText(extractor, station, alpha)
    }

    abstract class MinimapGraphicsExtractor(private val original: MinimapElementGraphics) : CommonGraphicsExtractor {
        override fun pushPose() = original.pose().pushPose()
        override fun popPose() = original.pose().popPose()
        override fun translate(x: Float, y: Float) = original.pose().translate(x, y, 0f)
        override fun scale(x: Float, y: Float) = original.pose().scale(x, y, 1f)
        override fun rotate(radians: Float) = original.pose().rotate(Axis.ZP, radians)
        override fun fill(x0: Int, y0: Int, x: Int, y: Int, color: Int) = original.fill(x0, y0, x, y, color)
        override fun centeredText(font: Font, text: String, x: Int, y: Int, color: Int) {
            original.drawCenteredString(font, text, x, y, color)
        }
    }
}
