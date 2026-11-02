package com.bleudev.simplemetro.custom.network.protocol.common.custom

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class ClientboundStationAnnouncement(
    val dimension: Identifier,
    val announcementId: String,
) : CustomPacketPayload {
    companion object : IPacketCompanion<ClientboundStationAnnouncement> {
        override val id = SMetro.resolve("packet/clientbound/station_announcement")
        override val codec: StreamCodec<RegistryFriendlyByteBuf, ClientboundStationAnnouncement> = StreamCodec.composite(
            Identifier.STREAM_CODEC, ClientboundStationAnnouncement::dimension,
            ByteBufCodecs.STRING_UTF8, ClientboundStationAnnouncement::announcementId,
            ::ClientboundStationAnnouncement,
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = type
}
