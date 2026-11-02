package com.bleudev.simplemetro.announcement

import com.bleudev.simplemetro.compat.ModCompat
import net.minecraft.server.level.ServerPlayer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AnnouncementTransportState {
    private val SVCPlayers = ConcurrentHashMap.newKeySet<UUID>()

    fun updateSVCPlayers(players: Collection<UUID>) {
        if (!ModCompat.SVC.present) return
        SVCPlayers.clear()
        SVCPlayers.addAll(players)
    }

    fun canPlayFor(playerId: UUID): Boolean = ModCompat.SVC.present && playerId in SVCPlayers

    fun playWithSVC(player: ServerPlayer, announcementId: String): Boolean {
        if (!canPlayFor(player.uuid)) return false
        if (!ModCompat.SVC.present) return false
        return runCatching { ModCompat.SVC.get().handleAnnouncement(player, announcementId) }.getOrDefault(false)
    }

    fun reset() {
        ModCompat.SVC.reset()
        SVCPlayers.clear()
    }
}
