package com.bleudev.simplemetro.compat

import net.minecraft.server.level.ServerPlayer

interface SVCCompat {
    fun handleAnnouncements(player: ServerPlayer, ids: List<String>): Boolean
    fun flushAnnouncementCache()
}