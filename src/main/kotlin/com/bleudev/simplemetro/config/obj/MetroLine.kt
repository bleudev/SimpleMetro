package com.bleudev.simplemetro.config.obj

import io.netty.buffer.ByteBuf
import kotlinx.serialization.Serializable
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

@Serializable
data class MetroLine(val id: Int, var name: String, var color: Int, var stations: List<Int>) {
    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MetroLine> = StreamCodec.composite(
            ByteBufCodecs.INT, MetroLine::id,
            ByteBufCodecs.STRING_UTF8, MetroLine::name,
            ByteBufCodecs.INT, MetroLine::color,
            ByteBufCodecs.list<ByteBuf, Int>().apply(ByteBufCodecs.INT), MetroLine::stations,
            ::MetroLine
        )
    }
}