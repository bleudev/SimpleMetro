package com.bleudev.simplemetro.compat

import net.minecraft.server.level.ServerPlayer

interface SVCCompat {
    fun handleAnnouncement(player: ServerPlayer, id: String): Boolean
    fun flushAnnouncementCache()
}