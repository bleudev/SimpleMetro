package com.bleudev.simplemetro.client.util

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.mixin.client.accessor.xaero.GuiMapAccessor
import com.bleudev.simplemetro.obj.MetroLine
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import xaero.map.gui.GuiMap
import java.lang.reflect.Modifier
import kotlin.math.*

@Environment(EnvType.CLIENT)
class MetroSchemeRenderer private constructor() {
    private var needsRebuild: Boolean = true
    private val stationRenderStates = hashMapOf<Int, StationRenderState>()
    private val connectionRenderStates = arrayListOf<ConnectionRenderState>()

    fun tick() {
        if (needsRebuild) {
            needsRebuild = false
            stationRenderStates.clear()
            connectionRenderStates.clear()

            val lmap = SMetroInfo.getInstance().lines.associateBy { it.id }
            for ((id, pos, name, transfers) in SMetroInfo.getInstance().stations) {
                stationRenderStates[id] = StationRenderState(id, pos, name, listOf(), transfers)
            }
            for (line in lmap.values) {
                for ((cr, nx) in line.stations.zipWithNext()) {
                    connectionRenderStates.add(ConnectionRenderState(stationRenderStates[cr]!!.pos, stationRenderStates[nx]!!.pos, line.color))

                    for (tr in stationRenderStates[cr]!!.transfers) {
                        connectionRenderStates.add(ConnectionRenderState(stationRenderStates[cr]!!.pos, stationRenderStates[tr]!!.pos, 0x555555))
                    }

                    stationRenderStates[cr] = stationRenderStates[cr]!!.addLine(line)
                    stationRenderStates[nx] = stationRenderStates[nx]!!.addLine(line)
                }
            }
        }
    }

    fun onUpdate() {
        needsRebuild = true
    }

    fun extractRenderState(map: GuiMap, extractor: GuiGraphicsExtractor, scaledMouseX: Int, scaledMouseY: Int) {
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
            fun doNotCull(drawX: Float, drawY: Float): Boolean {
                return !(drawX < -10.0) && !(drawY < -10.0) && !(drawX > (width + 10)) && !(drawY > (height + 10))
            }

            for (connection in connectionRenderStates) {
                drawConnection(extractor, connection, ::drawCoords)
            }
            var delayed: (() -> Unit)? = null
            for (station in stationRenderStates.values) {
                val (drawX, drawY) = drawCoords(station.pos.x, station.pos.z)
                if (doNotCull(drawX, drawY)) {
                    drawStation(extractor, station, drawX, drawY, scaledMouseX.toFloat(), scaledMouseY.toFloat(), pixelScale)?.let { delayed = it }
                }
            }
            delayed?.invoke()
        }
    }

    private fun drawStation(extractor: GuiGraphicsExtractor, station: StationRenderState, drawX: Float, drawY: Float, mouseX: Float, mouseY: Float, pixelScale: Double): (() -> Unit)? {
        if (station.lines.isEmpty() || station.argbColors.isEmpty()) return null

        extractor.pose().pushMatrix()
        extractor.pose().translate(drawX, drawY)

        // Support for only 4 colors, sorry
        fun checkSize(s: Int): Boolean = station.lines.size >= s && station.argbColors.size >= s
        extractor.fill(-4, -4, 4, 4, station.argbColors[0])
        if (checkSize(2)) {
            extractor.fill(-4, 0, 4, 4, station.argbColors[1])
            if (checkSize(3)) {
                extractor.fill(-4, 0, 0, 4, station.argbColors[2])
                if (checkSize(4)) {
                    extractor.fill(-4, -4, 0, 0, station.argbColors[3])
                }
            }
        }


        extractor.pose().pushMatrix()
        extractor.pose().scale(.75f)
        val alpha = ((pixelScale - 7.5) / 1.5).coerceIn(0.0, 1.0).toFloat()
        if (station.lines.size == 1) {
            extractor.centeredText(Minecraft.getInstance().font, station.lines[0].id.toString(), 0, -4, ARGB.color(alpha, 0xffffff))
            extractor.centeredText(Minecraft.getInstance().font, station.name, 0, 8, ARGB.srgbLerp(alpha, ARGB.color(0f, station.argbColors[0]), station.argbColors[0]))
        } else {
            extractor.centeredText(Minecraft.getInstance().font, station.name, 0, 8, ARGB.srgbLerp(alpha, ARGB.color(0f, station.whiteColor), station.whiteColor))
        }
        extractor.pose().popMatrix()
        extractor.pose().popMatrix()

        if ((drawX-mouseX).pow(2) + (drawY-mouseY).pow(2) <= 25) {
            return {
                extractor.pose().pushMatrix()
                extractor.pose().translate(mouseX, mouseY)
                extractor.stationTooltip(station)
                extractor.pose().popMatrix()
            }
        }
        return null
    }

    private fun GuiGraphicsExtractor.stationTooltip(station: StationRenderState) {
        val col = if (station.lines.size > 1) -1 else station.lines[0].color
        val lines = arrayListOf<ClientTooltipComponent>(ColoredTooltipComponent(station.name, col))
        for ((id, name, color) in station.lines) {
            lines.add(StationLineNameTooltipComponent(id, name, color))
        }
        lines.add(ColoredTooltipComponent("", -1)) // space
        lines.add(ClientTextTooltip(Component.translatable("xaero.map.station.tooltip.id", station.id).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC).visualOrderText))

        tooltip(Minecraft.getInstance().font, lines.toList(), 0, 0, DefaultTooltipPositioner.INSTANCE, null, true)
    }

    private fun drawConnection(extractor: GuiGraphicsExtractor, connection: ConnectionRenderState, drawCoords: (Int, Int) -> Pair<Float, Float>) {
        val x1 = connection.pos1.x
        val z1 = connection.pos1.z
        val x2 = connection.pos2.x
        val z2 = connection.pos2.z
        val col = connection.argbColor

        val (drawX1, drawY1) = drawCoords(x1, z1)
        val (drawX2, drawY2) = drawCoords(x2, z2)

        val dx = drawX2 - drawX1
        val dy = drawY2 - drawY1
        val len = hypot(dx, dy)

        extractor.pose().pushMatrix()
        if (dx > 0 || (dx == 0f && dy > 0)) {
            extractor.pose().translate(drawX1, drawY1)
        } else {
            extractor.pose().translate(drawX2, drawY2)
        }
        extractor.pose().rotate(if (dx == 0f) PI.toFloat()/2 else atan(dy / dx)) // No zero division error!
        extractor.fill(-2, -2, len.roundToInt() + 2, 2, col)
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

    private open class ColoredTooltipComponent(private val text: String, protected val color: Int) : ClientTooltipComponent {
        override fun getHeight(font: Font): Int = font.lineHeight + 1
        override fun getWidth(font: Font): Int = font.width(this.text)
        override fun extractText(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int) {
            val col = ARGB.setBrightness(ARGB.color(1f, color), .9f)
            graphics.text(font, this.text, x, y, col)
        }
    }

    private class StationLineNameTooltipComponent(private val id: Int, name: String, color: Int) : ColoredTooltipComponent(name, color) {
        override fun getWidth(font: Font): Int = super.getWidth(font) + 8
        override fun extractText(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int) {
            val col = ARGB.setBrightness(ARGB.color(1f, color), .9f)
            graphics.fill(x, y+1, x+6, y+7, col)
            graphics.pose().pushMatrix()
            graphics.pose().translate(x.toFloat(), y.toFloat())
            graphics.pose().scale(.5f)
            graphics.centeredText(font, id.toString(), 6, 4, -1)
            graphics.pose().popMatrix()
            super.extractText(graphics, font, x + 8, y)
        }
    }

    private data class StationLineRenderState(val id: Int, val name: String, val color: Int)

    private data class StationRenderState(val id: Int, val pos: BlockPos, val name: String, val lines: List<StationLineRenderState>, val transfers: List<Int>) {
        val argbColors: List<Int> get() = lines.map { ARGB.color(.9f, it.color) }
        val whiteColor: Int get() = ARGB.color(.9f, 0xffffff)

        fun addLine(line: MetroLine): StationRenderState = StationRenderState(
            id, pos, name,
            HashSet(lines).apply { add(StationLineRenderState(line.id, line.name, line.color)) }.toList(),
            transfers
        )
    }
    private data class ConnectionRenderState(val pos1: BlockPos, val pos2: BlockPos, val color: Int) {
        val argbColor: Int get() = ARGB.color(.8f, color)
    }
}