package com.bleudev.simplemetro.announcement

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AnnouncementTransportState {
    private val voicechatPlayers = ConcurrentHashMap.newKeySet<UUID>()

    @Volatile
    var voicechatAvailable: Boolean = false
        private set

    fun setVoicechatAvailable(available: Boolean) {
        voicechatAvailable = available
        if (!available) voicechatPlayers.clear()
    }

    fun updateVoicechatPlayers(players: Collection<UUID>) {
        if (!voicechatAvailable) return
        voicechatPlayers.clear()
        voicechatPlayers.addAll(players)
    }

    fun canPlayFor(playerId: UUID): Boolean = voicechatAvailable && playerId in voicechatPlayers

    fun reset() {
        voicechatAvailable = false
        voicechatPlayers.clear()
    }
}
