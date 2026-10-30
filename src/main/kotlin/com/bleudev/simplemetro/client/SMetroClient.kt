package com.bleudev.simplemetro.client

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.client.render.LinePanelRenderer
import com.bleudev.simplemetro.client.storage.MetroClientStorage
import com.bleudev.simplemetro.client.storage.MetroSchemeExtractor
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.custom.SMetroPacketPayloads
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ClientboundMetroInfoSync
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.level.storage.LevelResource

@Environment(EnvType.CLIENT)
class SMetroClient : ClientModInitializer {
    override fun onInitializeClient() {
        SMetroPacketPayloads.initializeClient()

        ClientPlayConnectionEvents.JOIN.register { l, _, mc ->
            val sd = l.serverData
            val sp = mc.singleplayerServer
            if (sp != null) {
                SMetroInfo.clientInit(sp.getWorldPath(LevelResource(SMetro.ID)))
            }
            else if (sd != null) {
                SMetroInfo.clientInit(FabricLoader.getInstance().configDir.resolve(SMetro.ID, sd.ip))
            }
            MetroSchemeExtractor.rebuild(SMetroInfo.getInstances())
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, context ->
            MClientCommand.register(dispatcher, context)
        }

        listen(ClientboundMetroInfoSync) {
            SMetroInfo.endSync(it.dimensionMap)
        }

        HudElementRegistry.addLast(SMetro.resolve("layer/table")) { e, _ ->
            LinePanelRenderer.extractRenderState(e)
        }

        ClientTickEvents.END_LEVEL_TICK.register { level ->
            val player = Minecraft.getInstance().player
            if (player != null) {
                MetroClientStorage.getInstance().onPlayerTick(player, level)
            }
        }
    }

    private fun <T : CustomPacketPayload> listen(payloadCompanion: IPacketCompanion<T>, handler: (payload: T, ctx: ClientPlayNetworking.Context) -> Unit) =
        ClientPlayNetworking.registerGlobalReceiver(payloadCompanion.type) {p, c -> handler(p, c)}
    private fun <T : CustomPacketPayload> listen(payloadCompanion: IPacketCompanion<T>, handler: (payload: T) -> Unit) =
        listen(payloadCompanion) {p, _ -> handler(p)}
}
