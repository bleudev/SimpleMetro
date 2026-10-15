package com.bleudev.simplemetro.client.render.xaero

import com.bleudev.simplemetro.client.render.MetroSchemeExtractor
import com.bleudev.simplemetro.obj.MetroStation
import com.mojang.math.Axis
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.TextColor
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3
import xaero.hud.minimap.element.render.MinimapElementGraphics
import kotlin.math.*

@Environment(EnvType.CLIENT)
object XaeroMinimapSchemeRenderer {
    private val renderState: MetroSchemeExtractor.SchemeRenderState get() = MetroSchemeExtractor.getInstance().extractRenderState()

    @JvmStatic
    fun render(
        graphics: MinimapElementGraphics,
        renderPos: Vec3,
        ps: Double,
        pc: Double,
        zoom: Double,
        halfViewW: Int,
        halfViewH: Int,
        circle: Boolean,
        minimapScale: Float,
        backgroundCoordinateScale: Double,
        playerCoordinateScale: Double,
    ) {
        val coordinateMultiplier = if (backgroundCoordinateScale == 0.0) {
            1.0
        } else {
            playerCoordinateScale / backgroundCoordinateScale
        }

        fun project(pos: BlockPos): Point {
            val offX = pos.x * coordinateMultiplier - renderPos.x
            val offZ = pos.z * coordinateMultiplier - renderPos.z
            val y = (pc * offX + ps * offZ) * zoom
            val x = (ps * offX - pc * offZ) * zoom
            return Point(x, y)
        }

        for (connection in renderState.connections) {
            val start = project(connection.pos1)
            val end = project(connection.pos2)
            val clipped = if (circle) {
                clipToCircle(start, end, min(halfViewW, halfViewH).toDouble())
            } else {
                clipToRectangle(start, end, halfViewW.toDouble(), halfViewH.toDouble())
            }
            if (clipped != null) {
                drawConnection(graphics, clipped.first, clipped.second, connection.argbColor, minimapScale)
            }
        }

        val font = Minecraft.getInstance().font
        for (station in renderState.stations.values) {
            val point = project(station.pos)
            val visible = if (circle) {
                point.x * point.x + point.y * point.y <= min(halfViewW, halfViewH).toDouble().pow(2)
            } else {
                abs(point.x) <= halfViewW.toDouble() && abs(point.y) <= halfViewH.toDouble()
            }
            if (visible) {
                drawStation(graphics, station, point, minimapScale, font)
            }
        }
    }

    private fun drawConnection(
        graphics: MinimapElementGraphics,
        first: Point,
        second: Point,
        color: Int,
        minimapScale: Float,
    ) {
        var start = first
        var end = second
        if (end.x < start.x || (end.x == start.x && end.y < start.y)) {
            val tmp = start
            start = end
            end = tmp
        }

        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = hypot(dx, dy)
        if (length < 0.01) return

        val halfThickness = max(1, (2.0 * minimapScale).roundToInt())
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(start.x.toFloat(), start.y.toFloat(), 0.0f)
        pose.rotate(Axis.ZP.rotation(atan2(dy, dx).toFloat()))
        graphics.fill(
            -halfThickness,
            -halfThickness,
            length.roundToInt() + halfThickness,
            halfThickness,
            color,
        )
        pose.popPose()
    }

    private fun drawStation(
        graphics: MinimapElementGraphics,
        station: MetroSchemeExtractor.StationRenderState,
        point: Point,
        minimapScale: Float,
        font: Font,
    ) {
        val pose = graphics.pose()
        val isOpen = station.status == MetroStation.Status.OPEN
        val colors = station.schemeColors

        pose.pushPose()
        pose.translate(point.x.toFloat(), point.y.toFloat(), 0.0f)
        pose.scale(minimapScale, minimapScale, 1.0f)

        if (isOpen) {
            graphics.fill(-4, -4, 4, 4, colors[0])
            if (station.lines.size >= 2 && colors.size >= 2) {
                graphics.fill(-4, 0, 4, 4, colors[1])
                if (station.lines.size >= 3 && colors.size >= 3) {
                    graphics.fill(-4, 0, 0, 4, colors[2])
                    if (station.lines.size >= 4 && colors.size >= 4) {
                        graphics.fill(-4, -4, 0, 0, colors[3])
                    }
                }
            }
        } else {
            graphics.fill(-4, -4, 4, 4, station.grayColor)
        }

        pose.pushPose()
        pose.scale(0.75f, 0.75f, 1.0f)
        if (isOpen) {
            if (station.lines.size == 1) {
                graphics.drawCenteredString(font, station.lines[0].id.toString(), 0, -4, ARGB.white(1.0f))
                graphics.drawCenteredString(font, station.name, 0, 8, ARGB.color(1.0f, station.lines[0].color))
            } else {
                graphics.drawCenteredString(font, station.name, 0, 8, ARGB.white(1.0f))
            }
        } else {
            graphics.drawCenteredString(font, station.name, 0, 8, ARGB.color(1.0f, TextColor.GRAY.value))
        }
        pose.popPose()
        pose.popPose()
    }

    private fun clipToRectangle(
        start: Point,
        end: Point,
        halfWidth: Double,
        halfHeight: Double,
    ): Pair<Point, Point>? {
        val dx = end.x - start.x
        val dy = end.y - start.y
        var t0 = 0.0
        var t1 = 1.0

        fun clip(p: Double, q: Double): Boolean {
            if (abs(p) < 1.0e-9) return q >= 0.0
            val r = q / p
            if (p < 0.0) {
                if (r > t1) return false
                if (r > t0) t0 = r
            } else {
                if (r < t0) return false
                if (r < t1) t1 = r
            }
            return true
        }

        if (!clip(-dx, start.x + halfWidth) ||
            !clip(dx, halfWidth - start.x) ||
            !clip(-dy, start.y + halfHeight) ||
            !clip(dy, halfHeight - start.y)
        ) return null

        return Point(start.x + t0 * dx, start.y + t0 * dy) to
            Point(start.x + t1 * dx, start.y + t1 * dy)
    }

    private fun clipToCircle(start: Point, end: Point, radius: Double): Pair<Point, Point>? {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val radiusSquared = radius * radius
        val startC = start.x * start.x + start.y * start.y - radiusSquared
        val endC = end.x * end.x + end.y * end.y - radiusSquared
        if (startC <= 0.0 && endC <= 0.0) return start to end

        val a = dx * dx + dy * dy
        if (a < 1.0e-9) return null
        val b = 2.0 * (start.x * dx + start.y * dy)
        val discriminant = b * b - 4.0 * a * startC
        if (discriminant < 0.0) return null

        val root = sqrt(discriminant)
        val tEnter = (-b - root) / (2.0 * a)
        val tExit = (-b + root) / (2.0 * a)
        val t0 = if (startC <= 0.0) 0.0 else max(0.0, tEnter)
        val t1 = if (endC <= 0.0) 1.0 else min(1.0, tExit)
        if (t0 > t1 || t1 < 0.0 || t0 > 1.0) return null

        val clippedStartT = t0.coerceIn(0.0, 1.0)
        val clippedEndT = t1.coerceIn(0.0, 1.0)
        return Point(start.x + clippedStartT * dx, start.y + clippedStartT * dy) to
            Point(start.x + clippedEndT * dx, start.y + clippedEndT * dy)
    }

    private data class Point(val x: Double, val y: Double)
}
