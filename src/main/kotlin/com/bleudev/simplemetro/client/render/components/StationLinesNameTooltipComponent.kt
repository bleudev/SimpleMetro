package com.bleudev.simplemetro.client.render.components

import com.bleudev.simplemetro.client.storage.MetroSchemeExtractor
import com.bleudev.simplemetro.config.obj.MetroStation
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.util.ARGB

internal class StationLinesNameTooltipComponent(
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