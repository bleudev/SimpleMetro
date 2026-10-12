package com.bleudev.simplemetro.obj

import net.minecraft.network.chat.TextColor

data class MetroLine(val id: Int, val name: String, val color: TextColor, val stations: List<Int>) {
}