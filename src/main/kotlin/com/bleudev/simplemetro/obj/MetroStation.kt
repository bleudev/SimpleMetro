package com.bleudev.simplemetro.obj

import net.minecraft.core.BlockPos

data class MetroStation(val id: Int, val pos: BlockPos, val name: String, val transfers: List<Int>) {

}