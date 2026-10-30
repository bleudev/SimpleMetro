package com.bleudev.simplemetro.util

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

fun ServerPlayer.sendPacket(payload: CustomPacketPayload) {
    if (ServerPlayNetworking.canSend(this, payload.type())) {
        ServerPlayNetworking.send(this, payload)
    }
}
