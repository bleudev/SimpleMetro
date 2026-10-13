package com.bleudev.simplemetro.util

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

fun ServerPlayer.sendPacket(payload: CustomPacketPayload) {
    ServerPlayNetworking.send(this, payload)
}
