package com.bleudev.simplemetro.server

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.custom.SMetroPacketPayloads
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ClientboundMetroInfoSync
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ServerboundMetroInfoEdit
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import com.bleudev.simplemetro.util.sendPacket
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.permissions.Permissions

class SMetroServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        SMetroPacketPayloads.initializeServer()

        ServerTickEvents.END_SERVER_TICK.register { server ->
            // Syncing
            val p = ClientboundMetroInfoSync(SMetroInfo.getInstance())
            for (player in server.playerList.players) player.sendPacket(p)
        }

        listen(ServerboundMetroInfoEdit) { payload, ctx ->
            if (ctx.player().permissions().hasPermission(Permissions.COMMANDS_ADMIN)) {
                SMetroInfo.save(payload.info)
            }
        }
    }

    private fun <T : CustomPacketPayload> listen(payloadCompanion: IPacketCompanion<T>, handler: (payload: T, ctx: ServerPlayNetworking.Context) -> Unit) =
        ServerPlayNetworking.registerGlobalReceiver(payloadCompanion.type) {p, c -> handler(p, c)}
    private fun <T : CustomPacketPayload> listen(payloadCompanion: IPacketCompanion<T>, handler: (payload: T) -> Unit) =
        listen(payloadCompanion) {p, _ -> handler(p)}
}