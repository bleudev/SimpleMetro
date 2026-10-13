@file:UseSerializers(BlockPosSerializer::class)
package com.bleudev.simplemetro.obj

import io.netty.buffer.ByteBuf
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

@Serializable
data class MetroStation(val id: Int, val pos: BlockPos, val name: String, val transfers: List<Int>) {
    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MetroStation> = StreamCodec.composite(
            ByteBufCodecs.INT, MetroStation::id,
            BlockPos.STREAM_CODEC, MetroStation::pos,
            ByteBufCodecs.STRING_UTF8, MetroStation::name,
            ByteBufCodecs.list<ByteBuf, Int>().apply(ByteBufCodecs.INT), MetroStation::transfers,
            ::MetroStation
        )
    }
}