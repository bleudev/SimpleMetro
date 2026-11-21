package com.bleudev.simplemetro.custom.network.protocol.common.custom

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import com.bleudev.simplemetro.util.SuggestionContext
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class ClientboundSuggestionContextSync(
    val context: SuggestionContext
) : CustomPacketPayload {
    companion object : IPacketCompanion<ClientboundSuggestionContextSync> {
        override val id = SMetro.resolve("packet/clientbound/sync/suggestion_context")
        override val codec: StreamCodec<RegistryFriendlyByteBuf, ClientboundSuggestionContextSync> = StreamCodec.composite(
            SuggestionContext.STREAM_CODEC, ClientboundSuggestionContextSync::context,
            ::ClientboundSuggestionContextSync
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = type
}
