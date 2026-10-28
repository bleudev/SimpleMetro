package com.bleudev.simplemetro.custom.network.protocol.common.custom

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class ServerboundMetroInfoEdit(val dimension: Identifier, val info: SMetroInfo) : CustomPacketPayload {
    companion object : IPacketCompanion<ServerboundMetroInfoEdit> {
        override val id = SMetro.resolve("packet/serverbound/edit/metro_info")
        override val codec: StreamCodec<RegistryFriendlyByteBuf, ServerboundMetroInfoEdit> = StreamCodec.composite(
            Identifier.STREAM_CODEC, ServerboundMetroInfoEdit::dimension,
            SMetroInfo.STREAM_CODEC, ServerboundMetroInfoEdit::info,
            ::ServerboundMetroInfoEdit
        )
    }
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = type
}