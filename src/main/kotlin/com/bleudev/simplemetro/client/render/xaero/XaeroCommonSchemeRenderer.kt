package com.bleudev.simplemetro.client.render.xaero

import com.bleudev.simplemetro.client.render.MetroSchemeExtractor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import kotlin.math.*

internal abstract class XaeroCommonSchemeRenderer {
    abstract val currentDimension: Identifier
    protected val renderState: MetroSchemeExtractor.SchemeRenderState get() = MetroSchemeExtractor.getInstance().extractRenderState(currentDimension)
    protected val pathRenderState: MetroSchemeExtractor.SchemeRenderState get() = MetroSchemeExtractor.getInstance().extractPathRenderState(currentDimension)

    protected fun extractRenderState(extractor: CommonGraphicsExtractor): Int? {
        for (connection in renderState.connections) {
            drawConnection(extractor, connection)
        }
        var hovered: Int? = null
        for (station in renderState.stations.values) {
            drawStation(extractor, station, false)
            if (extractor.mousePos().distanceSqr(extractor.drawPos(station.pos)) <= 25) {
                hovered = station.id
            }
        }
        // Path
        for (connection in pathRenderState.connections) {
            drawConnection(extractor, connection)
        }
        for (station in pathRenderState.stations.values) {
            drawStation(extractor, station, true)
        }
        return hovered
    }

    protected open fun drawConnection(extractor: CommonGraphicsExtractor, connection: MetroSchemeExtractor.ConnectionRenderState) {
        var start = extractor.drawPos(connection.pos1)
        var end = extractor.drawPos(connection.pos2)

        if (end.x < start.x || (end.x == start.x && end.y < start.y)) {
            val tmp = start
            start = end
            end = tmp
        }

        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = hypot(dx, dy)
        if (length < 0.01) return

        val halfThickness = max(1, (2.0 * extractor.mapScale()).roundToInt())

        extractor.pushPose()
        extractor.translate(start.x, start.y)
        extractor.rotate(atan2(dy, dx))
        extractor.fill(
            -halfThickness,
            -halfThickness,
            length.roundToInt() + halfThickness,
            halfThickness,
            ARGB.color(.8f, connection.color)
        )
        extractor.popPose()
    }

    protected open fun drawStation(extractor: CommonGraphicsExtractor, station: MetroSchemeExtractor.StationRenderState, selected: Boolean) {
        if (station.status.invisible) return

        val colors = station.schemeColors

        extractor.pushPose()
        extractor.translate(extractor.drawPos(station.pos))
        extractor.scale(extractor.mapScale())
        if (station.status.open) {
            // Only 4 colors(
            extractor.fill(-4, -4, 4, 4, colors[0])
            if (colors.size >= 2) {
                extractor.fill(-4, 0, 4, 4, colors[1])
                if (colors.size >= 3) {
                    extractor.fill(-4, 0, 0, 4, colors[2])
                    if (colors.size >= 4) {
                        extractor.fill(-4, -4, 0, 0, colors[3])
                    }
                }
            }
        } else {
            extractor.fill(-4, -4, 4, 4, station.grayColor)
        }

        if (!selected) drawStationText(extractor, station, 1f)
        extractor.popPose()
    }

    protected open fun drawStationText(extractor: CommonGraphicsExtractor, station: MetroSchemeExtractor.StationRenderState, alpha: Float) {
        val font = Minecraft.getInstance().font
        val lerp = { col: Int -> ARGB.srgbLerp(alpha, ARGB.color(0f, col), ARGB.color(1f, col)) }
        extractor.pushPose()
        extractor.scale(.75f)
        if (station.status.open) {
            if (station.lines.size == 1) {
                extractor.centeredText(font, station.lines[0].id.toString(), 0, -4, ARGB.white(alpha))
                extractor.centeredText(font, station.name, 0, 8, lerp(station.lines[0].color))
            } else {
                extractor.centeredText(font, station.name, 0, 8, ARGB.white(alpha))
            }
        } else {
            extractor.centeredText(font, station.name, 0, 8, lerp(TextColor.GRAY.value))
        }
        extractor.popPose()
    }

    interface CommonGraphicsExtractor {
        fun pushPose()
        fun popPose()
        fun translate(x: Float, y: Float)
        fun scale(x: Float, y: Float)
        fun rotate(radians: Float)
        fun fill(x0: Int, y0: Int, x: Int, y: Int, color: Int)
        fun centeredText(font: Font, text: String, x: Int, y: Int, color: Int)
        fun mapScale(): Float
        fun mousePos(): DrawPosition
        fun drawPos(x: Int, z: Int): DrawPosition
        // Implemented
        fun scale(xy: Float) = scale(xy, xy)
        fun translate(pos: DrawPosition) = translate(pos.x, pos.y)
        fun drawPos(pos: BlockPos): DrawPosition = drawPos(pos.x, pos.z)
    }

    data class DrawPosition(val x: Float, val y: Float) {
        fun distanceSqr(other: DrawPosition): Float {
            return (x-other.x).pow(2) + (y-other.y).pow(2)
        }
    }
}