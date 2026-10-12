package com.bleudev.simplemetro.client.util

import com.bleudev.simplemetro.mixin.client.accessor.xaero.GuiMapAccessor
import com.bleudev.simplemetro.obj.MetroLine
import com.bleudev.simplemetro.obj.MetroStation
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.TextColor
import net.minecraft.util.ARGB
import xaero.map.gui.GuiMap
import java.lang.reflect.Modifier
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.roundToInt

class MetroSchemeRenderer private constructor() {
    private val stations: List<MetroStation> = listOf(
        MetroStation(1, 1, BlockPos(0, 100, 0), "Грибная"),
        MetroStation(2, 1, BlockPos(100, 100, 50), "Портальная"),
        MetroStation(3, 1, BlockPos(150, 100, 50), "Какая-та еще"),
        MetroStation(4, 2, BlockPos(100, 100, 0), "Вафельная"),
        MetroStation(5, 2, BlockPos(75, 100, 150), "Вишнево"),
        MetroStation(6, 2, BlockPos(100, 100, 60), "Портальная"),
    )
    private val lines: List<MetroLine> = listOf(
        MetroLine(1, "Грибно-портальная ветка", TextColor.BLUE, listOf(1, 2, 3)),
        MetroLine(2, "Вафельная ветка", TextColor.GOLD, listOf(4, 6, 5)),
    )

    private var needsRebuild: Boolean = true
    private val stationRenderStates = hashMapOf<Int, StationRenderState>()
    private val connectionRenderStates = arrayListOf<ConnectionRenderState>()

    fun tick() {
        if (needsRebuild) {
            needsRebuild = false
            stationRenderStates.clear()
            connectionRenderStates.clear()

            val lmap = lines.associateBy { it.id }
            for ((id, lineId, pos, name) in stations) {
                stationRenderStates[id] = StationRenderState(pos, name, lmap[lineId]!!.color, lineId)
            }
            for ((_, _, color, stations) in lmap.values) {
                for ((pr, cr) in stations.zipWithNext()) {
                    connectionRenderStates.add(ConnectionRenderState(stationRenderStates[pr]!!.pos, stationRenderStates[cr]!!.pos, color))
                }
            }
        }
    }

    fun extractRenderState(map: GuiMap, extractor: GuiGraphicsExtractor) {
        val width = extractor.guiWidth()
        val height = extractor.guiHeight()

        val i = (map as GuiMapAccessor)

        val cameraX = i.`simplemetro$cameraX`()
        val cameraZ = i.`simplemetro$cameraZ`()
        var pixelScale = i.`simplemetro$scale`()
        if (pixelScale <= 0f) {
            pixelScale = i.`simplemetro$userScale`()
        }

        if (pixelScale > 0f) {
            val mapScreenScale = getIntField(map, "screenScale", 1).toDouble()
            val scale = pixelScale / mapScreenScale

            fun drawCoords(x: Int, z: Int): Pair<Float, Float> {
                val drawX = (x - cameraX) * scale + width / 2
                val drawY = (z - cameraZ) * scale + height / 2
                return drawX.toFloat() to drawY.toFloat()
            }

            for (connection in connectionRenderStates) {
                drawConnection(extractor, connection, ::drawCoords)
            }
            for (station in stationRenderStates.values) {
                val (drawX, drawY) = drawCoords(station.pos.x, station.pos.z)
                if (!(drawX < -10.0) && !(drawY < -10.0) && !(drawX > (width + 10)) && !(drawY > (height + 10))) {
                    drawStation(extractor, station, drawX, drawY, pixelScale)
                }
            }
        }
    }

    private fun drawStation(extractor: GuiGraphicsExtractor, station: StationRenderState, drawX: Float, drawY: Float, pixelScale: Double) {
        extractor.pose().pushMatrix()
        extractor.pose().translate(drawX, drawY)
        extractor.fill(-4, -4, 4, 4, station.argbColor)

        extractor.pose().pushMatrix()
        extractor.pose().scale(.75f)
        val alpha = ((pixelScale - 7.5) / 1.5).coerceIn(0.0, 1.0).toFloat()
        extractor.centeredText(Minecraft.getInstance().font, station.lineId.toString(), 0, -4, ARGB.color(alpha, 0xffffff))
        extractor.centeredText(Minecraft.getInstance().font, station.name, 0, 8, ARGB.srgbLerp(alpha, ARGB.color(0f, station.argbColor), station.argbColor))
        extractor.pose().popMatrix()

        extractor.pose().popMatrix()
    }

    private fun drawConnection(extractor: GuiGraphicsExtractor, connection: ConnectionRenderState, drawCoords: (Int, Int) -> Pair<Float, Float>) {
        val (drawX1, drawY1) = drawCoords(connection.pos1.x, connection.pos1.z)
        val (drawX2, drawY2) = drawCoords(connection.pos2.x, connection.pos2.z)
        val dx = drawX2 - drawX1
        val dy = drawY2 - drawY1
        val len = hypot(dx, dy)

        extractor.pose().pushMatrix()
        extractor.pose().translate(drawX1, drawY1)
        extractor.pose().rotate(acos(dx / len))
        extractor.fill(-2, -2, len.roundToInt() + 2, 2, ARGB.color(.9f, connection.color.value))
        extractor.pose().popMatrix()
    }

    @Suppress("SameParameterValue")
    private fun getIntField(target: Any, fieldName: String, fallback: Int): Int {
        try {
            var cursor = target.javaClass
            @Suppress("SENSELESS_COMPARISON")
            while (cursor != null) {
                try {
                    val field = cursor.getDeclaredField(fieldName)
                    field.isAccessible = true
                    val owner = if (Modifier.isStatic(field.modifiers)) null else target
                    val value = field.get(owner)
                    if (value is Number) return value.toInt()
                    break
                } catch (_: Exception) {}
                cursor = cursor.getSuperclass()
            }
        } catch (_: Exception) {}
        return fallback
    }

    companion object {
        private var instance: MetroSchemeRenderer? = null

        @JvmStatic
        fun getInstance(): MetroSchemeRenderer {
            if (instance == null) {
                instance = MetroSchemeRenderer()
            }
            return instance!!
        }
    }

    private data class StationRenderState(val pos: BlockPos, val name: String, val color: TextColor, val lineId: Int) {
        val argbColor: Int get() = ARGB.color(.9f, color.value)
    }
    private data class ConnectionRenderState(val pos1: BlockPos, val pos2: BlockPos, val color: TextColor)
}