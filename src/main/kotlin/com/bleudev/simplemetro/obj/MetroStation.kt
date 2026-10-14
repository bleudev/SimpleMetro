@file:UseSerializers(BlockPosSerializer::class)
package com.bleudev.simplemetro.obj

import io.netty.buffer.ByteBuf
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.StringRepresentable

@Serializable
data class MetroStation(val id: Int, var pos: BlockPos, var name: String, val transfers: List<Int> = listOf(), var status: Status = Status.OPEN) {
    enum class Status(val message: Component? = null) : StringRepresentable {
        OPEN,
        CLOSED_TEMP(Component.translatable("xaero.map.station.tooltip.status.closed_temp")),
        CLOSED_REPAIR(Component.translatable("xaero.map.station.tooltip.status.closed_repair")),
        UNDER_CONSTRUCTION(Component.translatable("xaero.map.station.tooltip.status.under_construction")),
        PLANNED(Component.translatable("xaero.map.station.tooltip.status.planned"));

        override fun getSerializedName(): String = name.lowercase()

        companion object {
            fun fromNameOrNull(name: String): Status? = entries.toList().firstOrNull { it.serializedName == name }
            fun fromName(name: String): Status = fromNameOrNull(name) ?: throw NoSuchElementException("Unknown status with name $name")
            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Status> = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Status::getSerializedName,
                ::fromName
            )
        }
    }

    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MetroStation> = StreamCodec.composite(
            ByteBufCodecs.INT, MetroStation::id,
            BlockPos.STREAM_CODEC, MetroStation::pos,
            ByteBufCodecs.STRING_UTF8, MetroStation::name,
            ByteBufCodecs.list<ByteBuf, Int>().apply(ByteBufCodecs.INT), MetroStation::transfers,
            Status.STREAM_CODEC, MetroStation::status,
            ::MetroStation
        )
    }
}