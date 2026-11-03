package com.bleudev.simplemetro.announcement.client

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.announcement.AnnouncementId
import com.bleudev.simplemetro.announcement.AnnouncementResolver
import com.bleudev.simplemetro.client.storage.MetroClientStorage
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.Announcements
import com.bleudev.simplemetro.util.isModOnServer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import java.nio.file.Files
import java.nio.file.Path

@Environment(EnvType.CLIENT)
object StationAnnouncementClient {
    private var activeSound: AnnouncementSoundInstance? = null

    fun play(dimension: Identifier, event: MetroClientStorage.StationEvent) {
        if (!shouldPlayLocally()) return
        val stationId = when (event.type) {
            Announcements.AnnouncementType.ARRIVAL -> event.stationId
            Announcements.AnnouncementType.DEPARTURE -> event.announcementStationId
        } ?: return
        playFiles(AnnouncementResolver.resolve(SMetroInfo.getInstance(dimension), stationId, event.lineId, event.type))
    }

    fun playServer(announcementIds: List<String>) {
        playFiles(announcementIds)
    }

    private fun shouldPlayLocally(): Boolean = !isModOnServer

    private fun playFiles(announcementIds: List<String>) {
        if (announcementIds.firstOrNull()?.let(AnnouncementId::isValid) != true) return

        val directory = SMetroInfo.announcementsDirectory.toAbsolutePath().normalize()
        val paths = ArrayList<Path>(announcementIds.size)
        for ((index, announcementId) in announcementIds.withIndex()) {
            if (!AnnouncementId.isValid(announcementId)) continue
            val file = directory.resolve("$announcementId.wav").normalize()
            if (file.parent != directory || !Files.isRegularFile(file)) {
                SMetro.LOGGER.debug("Station announcement WAV is missing on the client: {}", file)
                if (index == 0) return
                continue
            }
            paths.add(file)
        }
        if (paths.isEmpty()) return

        try {
            val soundManager = Minecraft.getInstance().soundManager
            val sound = AnnouncementSoundInstance(paths)
            activeSound?.let(soundManager::stop)
            activeSound = sound
            soundManager.play(sound)
        } catch (exception: Exception) {
            SMetro.LOGGER.warn("Could not play station announcement sequence {}", announcementIds, exception)
        }
    }
}
