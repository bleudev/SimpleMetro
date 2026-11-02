package com.bleudev.simplemetro.custom.network.protocol.common.custom

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class ClientboundMetroInfoSync(val dimensionMap: Map<Identifier, SMetroInfo>) : CustomPacketPayload {
    companion object : IPacketCompanion<ClientboundMetroInfoSync> {
        override val id = SMetro.resolve("packet/clientbound/sync/metro_info")
        override val codec: StreamCodec<RegistryFriendlyByteBuf, ClientboundMetroInfoSync> = StreamCodec.composite(
            ByteBufCodecs.map(::HashMap, Identifier.STREAM_CODEC, SMetroInfo.STREAM_CODEC), ClientboundMetroInfoSync::dimensionMap,
            ::ClientboundMetroInfoSync
        )
    }
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = type
}