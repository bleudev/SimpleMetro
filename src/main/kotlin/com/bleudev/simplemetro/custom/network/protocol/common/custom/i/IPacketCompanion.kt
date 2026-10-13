package com.bleudev.simplemetro.custom.network.protocol.common.custom.i

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

interface IPacketCompanion<T : CustomPacketPayload> {
    val id: Identifier
    val codec: StreamCodec<RegistryFriendlyByteBuf, T>

    val type: CustomPacketPayload.Type<T> get() = CustomPacketPayload.Type(id)
}