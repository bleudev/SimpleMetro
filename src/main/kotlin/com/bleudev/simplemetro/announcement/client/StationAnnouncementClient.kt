package com.bleudev.simplemetro.announcement.client

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.announcement.AnnouncementId
import com.bleudev.simplemetro.client.storage.MetroClientStorage
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.util.isModOnServer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import java.nio.file.Files

@Environment(EnvType.CLIENT)
object StationAnnouncementClient {
    fun play(dimension: Identifier, event: MetroClientStorage.StationEvent) {
        if (shouldPlayLocally()) return
        playNow(dimension, event)
    }

    fun playServer(announcementId: String) {
        playFile(announcementId)
    }

    private fun shouldPlayLocally(): Boolean = !isModOnServer

    private fun playNow(dimension: Identifier, event: MetroClientStorage.StationEvent) {
        val stationId = when (event.type) {
            MetroClientStorage.AnnouncementType.ARRIVAL -> event.stationId
            MetroClientStorage.AnnouncementType.DEPARTURE -> event.announcementStationId
        } ?: return
        val station = SMetroInfo.getInstance(dimension).stations.firstOrNull { it.id == stationId } ?: return
        var announcementId = station.announcements.get(event.type)
        if (announcementId == null) {
            val line = SMetroInfo.getInstance(dimension).lines.firstOrNull { it.id == event.lineId } ?: return
            announcementId = line.announcements.get(event.type) ?: return
        }
        playFile(announcementId)
    }

    private fun playFile(announcementId: String) {
        if (!AnnouncementId.isValid(announcementId)) return
        val directory = SMetroInfo.announcementsDirectory.toAbsolutePath().normalize()
        val file = directory.resolve("$announcementId.wav").normalize()
        if (file.parent != directory || !Files.isRegularFile(file)) {
            SMetro.LOGGER.debug("Station announcement WAV is missing on the client: {}", file)
            return
        }

        try {
            Minecraft.getInstance().soundManager.play(AnnouncementSoundInstance(file))
        } catch (exception: Exception) {
            SMetro.LOGGER.warn("Could not play station announcement '{}'", announcementId, exception)
        }
    }
}
