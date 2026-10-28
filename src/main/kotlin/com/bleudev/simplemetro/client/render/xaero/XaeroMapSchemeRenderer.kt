package com.bleudev.simplemetro.client.render.xaero

import com.bleudev.simplemetro.client.render.MetroSchemeExtractor
import com.bleudev.simplemetro.config.obj.MetroStation
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.util.FormattedCharSequence
import java.lang.reflect.Modifier

@Environment(EnvType.CLIENT)
internal object XaeroMapSchemeRenderer : XaeroCommonSchemeRenderer() {
    private var dim: Identifier? = null
    override val currentDimension: Identifier
        get() = dim ?: ClientLevel.OVERWORLD.identifier()

    private var width: Int = 0
    private var height: Int = 0
    private var pixelScale: Float = 0f

    @JvmStatic
    internal fun extractRenderState(
        extractor: GuiGraphicsExtractor,
        scaledMouseX: Int,
        scaledMouseY: Int,
        cameraX: Double,
        cameraZ: Double,
        pixelScale: Double,
        mapScreenScale: Double,
        dimension: Identifier,
    ) {
        this.width = extractor.guiWidth()
        this.height = extractor.guiHeight()
        this.pixelScale = pixelScale.toFloat()
        this.dim = dimension

        if (pixelScale > 0) {
            val scale = pixelScale / mapScreenScale

            val adapter = object : MapGraphicsExtractor(extractor) {
                override fun mapScale(): Float = 1f
                override fun mousePos(): DrawPosition = DrawPosition(scaledMouseX.toFloat(), scaledMouseY.toFloat())
                override fun drawPos(x: Int, z: Int): DrawPosition {
                    val x = (x - cameraX) * scale + width / 2
                    val y = (z - cameraZ) * scale + height / 2
                    return DrawPosition(x.toFloat(), y.toFloat())
                }
            }
            extractRenderState(adapter)?.let { hovered ->
                drawStationTooltip(extractor, renderState.stations[hovered]!!, scaledMouseX.toFloat(), scaledMouseY.toFloat())
            }
        }
    }

    override fun drawStation(extractor: CommonGraphicsExtractor, station: MetroSchemeExtractor.StationRenderState) {
        val drawPos = extractor.drawPos(station.pos)
        if (!(drawPos.x < -10.0) && !(drawPos.y < -10.0) && !(drawPos.x > (width + 10)) && !(drawPos.y > (height + 10))) {
            super.drawStation(extractor, station)
        }
    }

    override fun drawStationText(
        extractor: CommonGraphicsExtractor,
        station: MetroSchemeExtractor.StationRenderState,
        alpha: Float
    ) {
        val alpha = ((this.pixelScale - 7.5) / 1.5).coerceIn(0.0, 1.0).toFloat()
        super.drawStationText(extractor, station, alpha)
    }

    private fun drawStationTooltip(
        extractor: GuiGraphicsExtractor,
        station: MetroSchemeExtractor.StationRenderState,
        mouseX: Float,
        mouseY: Float,
    ) {
        if (station.status.invisible) return

        extractor.pose().pushMatrix()
        extractor.pose().translate(mouseX, mouseY)

        val space = ClientTextTooltip(FormattedCharSequence.EMPTY)

        val lines = arrayListOf<ClientTooltipComponent>(StationLinesNameTooltipComponent(station.lines, station.name, station.status))
        if (station.status.message != null) {
            lines.add(ClientTextTooltip(station.status.message.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC).visualOrderText))
        }
        lines.add(space)
        for (line in station.lines) {
            lines.add(StationLinesNameTooltipComponent(listOf(line), line.name, MetroStation.Status.OPEN))
        }
        if (station.lines.isNotEmpty()) lines.add(space)

        if (station.transfers.isNotEmpty()) {
            lines.add(ClientTextTooltip((
                    if (station.transfers.size == 1) Component.translatable("xaero.map.station.tooltip.transfers.one")
                    else Component.translatable("xaero.map.station.tooltip.transfers.some")
                    ).visualOrderText))
            for (transfer in station.transfers) {
                renderState.stations[transfer]?.let { state ->
                    lines.add(StationLinesNameTooltipComponent(state.lines, state.name, state.status))
                }
            }
            lines.add(space)
        }

        lines.add(ClientTextTooltip(Component.translatable("xaero.map.station.tooltip.id", station.id).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC).visualOrderText))

        extractor.tooltip(Minecraft.getInstance().font, lines.toList(), 0, 0, DefaultTooltipPositioner.INSTANCE, null, false)

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

    private class StationLinesNameTooltipComponent(
        lines: List<MetroSchemeExtractor.StationLineRenderState>,
        private val name: String,
        private val status: MetroStation.Status,
    ) : ClientTooltipComponent {
        private val lines: List<MetroSchemeExtractor.StationLineRenderState> = lines.sortedBy { it.id }

        override fun getHeight(font: Font): Int = font.lineHeight + 1
        override fun getWidth(font: Font): Int = font.width(name) + 8 * lines.size
        override fun extractText(graphics: GuiGraphicsExtractor, font: Font, x: Int, y: Int) {
            var x = x
            var styledName = Component.literal(name)
            var nameColor = ARGB.color(1f, if (status.open) if (lines.size == 1) lines[0].color else 0xffffff else TextColor.GRAY.value)
            if (status.open) {
                nameColor = ARGB.setBrightness(nameColor, .9f)
            } else {
                styledName = styledName.withStyle(ChatFormatting.ITALIC)
            }

            for ((lineId, _, lineColor) in lines) {
                val col = ARGB.setBrightness(ARGB.color(1f, lineColor), .9f)
                graphics.fill(x, y+1, x+6, y+7, col)
                graphics.pose().pushMatrix()
                graphics.pose().translate(x.toFloat(), y.toFloat())
                graphics.pose().scale(.5f)
                graphics.centeredText(font, lineId.toString(), 6, 4, -1)
                graphics.pose().popMatrix()
                x += 8
            }
            graphics.text(font, styledName, x, y, nameColor)
        }
    }

    abstract class MapGraphicsExtractor(private val original: GuiGraphicsExtractor) : CommonGraphicsExtractor {
        override fun pushPose() { original.pose().pushMatrix() }
        override fun popPose() { original.pose().popMatrix() }
        override fun translate(x: Float, y: Float) { original.pose().translate(x, y) }
        override fun scale(x: Float, y: Float) { original.pose().scale(x, y) }
        override fun rotate(radians: Float) { original.pose().rotate(radians) }
        override fun fill(x0: Int, y0: Int, x: Int, y: Int, color: Int) = original.fill(x0, y0, x, y, color)
        override fun centeredText(font: Font, text: String, x: Int, y: Int, color: Int) = original.centeredText(font, text, x, y, color)
    }
}