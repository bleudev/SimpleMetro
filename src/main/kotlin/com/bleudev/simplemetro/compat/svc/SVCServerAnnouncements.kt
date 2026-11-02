package com.bleudev.simplemetro.compat.svc

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.announcement.AnnouncementTransportState
import com.bleudev.simplemetro.compat.ModCompat
import com.bleudev.simplemetro.config.SMetroInfo
import de.maxhenkel.voicechat.api.VoicechatConnection
import de.maxhenkel.voicechat.api.VoicechatServerApi
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import java.util.*

internal object SVCServerAnnouncements {

    private data class PlayerState(
        val dimension: Identifier,
        val stationId: Int?,
    )

    private val playerStates = HashMap<UUID, PlayerState>()
    private var api: VoicechatServerApi? = null
    private var started = false
    private var initializedServer: MinecraftServer? = null

    fun start(serverApi: VoicechatServerApi) {
        api = serverApi
        ModCompat.SVC.initialize(SVCCompatImpl(serverApi))
        if (started) return
        started = true

        ServerTickEvents.START_SERVER_TICK.register(::updateSVCPlayers)
        ServerLifecycleEvents.SERVER_STOPPED.register {
            playerStates.clear()
            ModCompat.SVC { flushAnnouncementCache() }
            api = null
            initializedServer = null
            AnnouncementTransportState.reset()
        }
    }

    private fun updateSVCPlayers(server: MinecraftServer) {
        val svc = api ?: return
        if (initializedServer != server) {
            SMetroInfo.serverInit(server)
            initializedServer = server
            playerStates.clear()
            ModCompat.SVC { flushAnnouncementCache() }
        }

        val onlinePlayers = server.playerList.players
        val connectedPlayers = onlinePlayers.mapNotNull { player ->
            runCatching<VoicechatConnection?> { svc.getConnectionOf(player.gameProfile.id) }
            .onFailure { exception ->
                SMetro.LOGGER.debug("Could not query Simple Voice Chat connection for {}", player.uuid, exception)
            }
            .getOrNull()
            ?.let { player.uuid }
        }
        AnnouncementTransportState.updateSVCPlayers(connectedPlayers)
    }
}
