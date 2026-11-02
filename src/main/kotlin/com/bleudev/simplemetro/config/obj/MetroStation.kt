@file:UseSerializers(BlockPosSerializer::class)

package com.bleudev.simplemetro.config.obj

import io.netty.buffer.ByteBuf
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.StringRepresentable
import java.util.*

@Serializable
data class MetroStation(val id: Int, var pos: BlockPos, var name: String, var transfers: List<Int> = listOf(), var status: Status = Status.OPEN, var arrivalAnnouncement: String? = null, var departureAnnouncement: String? = null) {
    @Serializable(StationStatusSerializer::class)
    enum class Status(val message: Component? = null) : StringRepresentable {
        OPEN,
        CLOSED_TEMP(Component.translatable("xaero.map.station.tooltip.status.closed_temp")),
        CLOSED_REPAIR(Component.translatable("xaero.map.station.tooltip.status.closed_repair")),
        UNDER_CONSTRUCTION(Component.translatable("xaero.map.station.tooltip.status.under_construction")),
        PLANNED(Component.translatable("xaero.map.station.tooltip.status.planned")),
        INVISIBLE,
        UNKNOWN(Component.translatable("xaero.map.station.tooltip.status.unknown"));

        val open: Boolean get() = this == OPEN
        val invisible: Boolean get() = this == INVISIBLE
        val unknown: Boolean get() = this == UNKNOWN

        override fun getSerializedName(): String = name.lowercase()

        companion object {
            fun fromNameOrNull(name: String): Status? = entries.toList().firstOrNull { it.serializedName == name }
            fun fromNameOrFallback(name: String): Status = fromNameOrNull(name) ?: UNKNOWN
            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Status> = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Status::getSerializedName,
                ::fromNameOrFallback
            )
        }
    }

    companion object {
        private val OPTIONAL_STRING_CODEC = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MetroStation> = StreamCodec.composite(
            ByteBufCodecs.INT, MetroStation::id,
            BlockPos.STREAM_CODEC, MetroStation::pos,
            ByteBufCodecs.STRING_UTF8, MetroStation::name,
            ByteBufCodecs.list<ByteBuf, Int>().apply(ByteBufCodecs.INT), MetroStation::transfers,
            Status.STREAM_CODEC, MetroStation::status,
            OPTIONAL_STRING_CODEC, { Optional.ofNullable(it.arrivalAnnouncement) },
            OPTIONAL_STRING_CODEC, { Optional.ofNullable(it.departureAnnouncement) },
        ) { id, pos, name, transfers, status, arrivalAnnouncement, departureAnnouncement ->
            MetroStation(
                id,
                pos,
                name,
                transfers,
                status,
                arrivalAnnouncement.orElse(null),
                departureAnnouncement.orElse(null),
            )
        }
    }
}