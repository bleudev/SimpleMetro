package com.bleudev.simplemetro.custom

import com.bleudev.simplemetro.custom.network.protocol.common.custom.ClientboundMetroInfoSync
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ServerboundMetroInfoEdit
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

object SMetroPacketPayloads {
    private fun <T : CustomPacketPayload> serverbound(packet: IPacketCompanion<T>) =
        PayloadTypeRegistry.serverboundPlay().register(packet.type, packet.codec)
    private fun <T : CustomPacketPayload> clientbound(packet: IPacketCompanion<T>) =
        PayloadTypeRegistry.clientboundPlay().register(packet.type, packet.codec)

    fun initialize() {
        clientbound(ClientboundMetroInfoSync)
    }

    fun initializeServer() {
        serverbound(ServerboundMetroInfoEdit)
    }

    fun initializeClient() {
        serverbound(ServerboundMetroInfoEdit)
    }
}