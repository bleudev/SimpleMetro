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

    fun playWithSVC(player: ServerPlayer, announcementIds: List<String>): Boolean {
        if (announcementIds.isEmpty() || announcementIds.any { !AnnouncementId.isValid(it) }) return false
        if (!canPlayFor(player.uuid) || !ModCompat.SVC.present) return false
        return runCatching { ModCompat.SVC.get().handleAnnouncements(player, announcementIds) }.getOrDefault(false)
    }

    fun reset() {
        ModCompat.SVC { flushAnnouncementCache() }
        ModCompat.SVC.reset()
        SVCPlayers.clear()
    }
}
