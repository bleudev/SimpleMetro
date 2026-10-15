package com.bleudev.simplemetro.client

import com.bleudev.simplemetro.client.util.MetroSchemeRenderer
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.custom.SMetroPacketPayloads
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ClientboundMetroInfoSync
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class SMetroClient : ClientModInitializer {
    override fun onInitializeClient() {
        SMetroPacketPayloads.initializeClient()

        ClientTickEvents.END_CLIENT_TICK.register { MetroSchemeRenderer.getInstance().tick() }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> MetroSchemeRenderer.getInstance().onUpdate() }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, context ->
            MClientCommand.register(dispatcher, context)
        }

        listen(ClientboundMetroInfoSync) {
            SMetroInfo.endSync(it.info)
        }
    }

    private fun <T : CustomPacketPayload> listen(payloadCompanion: IPacketCompanion<T>, handler: (payload: T, ctx: ClientPlayNetworking.Context) -> Unit) =
        ClientPlayNetworking.registerGlobalReceiver(payloadCompanion.type) {p, c -> handler(p, c)}
    private fun <T : CustomPacketPayload> listen(payloadCompanion: IPacketCompanion<T>, handler: (payload: T) -> Unit) =
        listen(payloadCompanion) {p, _ -> handler(p)}
}
