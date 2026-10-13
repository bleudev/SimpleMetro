package com.bleudev.simplemetro.custom.network.protocol.common.custom

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class ClientboundMetroInfoSync(val info: SMetroInfo) : CustomPacketPayload {
    companion object : IPacketCompanion<ClientboundMetroInfoSync> {
        override val id = SMetro.resolve("packet/clientbound/sync/metro_info")
        override val codec: StreamCodec<RegistryFriendlyByteBuf, ClientboundMetroInfoSync> = StreamCodec.composite(
            SMetroInfo.STREAM_CODEC, ClientboundMetroInfoSync::info,
            ::ClientboundMetroInfoSync
        )
    }
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = type
}