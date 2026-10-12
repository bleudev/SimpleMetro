package com.bleudev.simplemetro.obj

import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos

data class MetroStation(val id: Int, val lineId: Int, val pos: BlockPos, val name: String) {

}