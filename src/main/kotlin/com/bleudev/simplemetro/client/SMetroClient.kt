package com.bleudev.simplemetro.client

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.announcement.client.StationAnnouncementClient
import com.bleudev.simplemetro.client.render.LinePanelRenderer
import com.bleudev.simplemetro.client.storage.MetroClientStorage
import com.bleudev.simplemetro.client.storage.MetroSchemeExtractor
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.custom.SMetroPacketPayloads
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ClientboundMetroInfoSync
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ClientboundStationAnnouncement
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ClientboundSuggestionContextSync
import com.bleudev.simplemetro.custom.network.protocol.common.custom.i.IPacketCompanion
import com.bleudev.simplemetro.util.SuggestionContext
import com.bleudev.simplemetro.util.isModOnServer
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
    private var hadActiveRoute = false

    override fun onInitializeClient() {
        SMetroPacketPayloads.initializeClient()

        ClientPlayConnectionEvents.JOIN.register { l, _, mc ->
            hadActiveRoute = false
            val sd = l.serverData
            val sp = mc.singleplayerServer
            if (sp != null) {
                SMetroInfo.clientInit(sp.getWorldPath(LevelResource(SMetro.ID)))
            }
            else if (sd != null) {
                SMetroInfo.clientInit(FabricLoader.getInstance().configDir.resolve(SMetro.ID, sd.ip))
            }
            val dimensions = SMetroInfo.getInstances()
            MetroSchemeExtractor.rebuild(dimensions)
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, context ->
            MClientCommand.register(dispatcher, context)
        }

        listen(ClientboundSuggestionContextSync) {
            SuggestionContext.sync(it.context)
        }

        listen(ClientboundMetroInfoSync) {
            SMetroInfo.endSync(it.dimensionMap)
        }

        listen(ClientboundStationAnnouncement) {
            StationAnnouncementClient.playServer(it.announcementIds)
        }

        HudElementRegistry.addLast(SMetro.resolve("layer/table")) { e, _ ->
            LinePanelRenderer.extractRenderState(e)
        }

        ClientTickEvents.END_LEVEL_TICK.register { level ->
            val player = Minecraft.getInstance().player
            if (player != null) {
                val storage = MetroClientStorage.getInstance()
                storage.onPlayerTick(player, level)
                val hasActiveRoute = storage.currentLine != null &&
                    storage.prevVisibleStation != null && storage.nextVisibleStation != null
                if (hadActiveRoute && !hasActiveRoute && !isModOnServer) {
                    StationAnnouncementClient.stop()
                }
                hadActiveRoute = hasActiveRoute
                while (true) {
                    val event = storage.pollSoundEvent() ?: break
                    StationAnnouncementClient.play(level.dimension().identifier(), event)
                }
            }
        }
    }

    private fun <T : CustomPacketPayload> listen(payloadCompanion: IPacketCompanion<T>, handler: (payload: T, ctx: ClientPlayNetworking.Context) -> Unit) =
        ClientPlayNetworking.registerGlobalReceiver(payloadCompanion.type) {p, c -> handler(p, c)}
    private fun <T : CustomPacketPayload> listen(payloadCompanion: IPacketCompanion<T>, handler: (payload: T) -> Unit) =
        listen(payloadCompanion) {p, _ -> handler(p)}
}
