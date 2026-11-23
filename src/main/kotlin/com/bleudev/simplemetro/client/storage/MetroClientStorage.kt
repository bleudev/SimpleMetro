package com.bleudev.simplemetro.client.storage

import com.bleudev.simplemetro.config.obj.Announcements
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.SharedConstants.MILLIS_PER_TICK
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import java.util.*
import kotlin.math.roundToInt

@Environment(EnvType.CLIENT)
class MetroClientStorage private constructor() {
    var currentStation: Int? = null
    var prevVisibleStation: Int? = null
    var nextVisibleStation: Int? = null

    var currentLine: Int? = null
    var stationProgress: Float? = null
    var distanceToNextStation: Double? = null
    var timeToNextStationMillis: Int? = null

    private val stationEvents = ArrayDeque<StationEvent>()
    private val soundEvents = ArrayDeque<StationEvent>()
    private var nextEventId = 0L

    fun onPlayerTick(player: LocalPlayer, level: ClientLevel) {
        LinePanelExtractor.getInstance().playerTick(this, player, level)

        // Time to next station calculating
        val dist = distanceToNextStation
        val playerSpeed = Minecraft.getInstance().cameraEntity?.knownSpeed?.length()
        timeToNextStationMillis = if (dist != null && playerSpeed != null && playerSpeed != 0.0) {
            (dist * MILLIS_PER_TICK / playerSpeed).roundToInt()
        } else null
    }

    fun publishStationEvent(type: Announcements.AnnouncementType, stationId: Int, stationName: String, lineId: Int?, announcementStationId: Int? = stationId) {
        if (stationEvents.size >= MAX_QUEUED_STATION_EVENTS) stationEvents.removeFirst()
        if (soundEvents.size >= MAX_QUEUED_STATION_EVENTS) soundEvents.removeFirst()
        val event = StationEvent(++nextEventId, type, stationId, stationName, lineId, announcementStationId)
        stationEvents.addLast(event)
        soundEvents.addLast(event)
    }

    fun pollStationEvent(): StationEvent? = stationEvents.pollFirst()
    fun pollSoundEvent(): StationEvent? = soundEvents.pollFirst()

    data class StationEvent(
        val id: Long,
        val type: Announcements.AnnouncementType,
        val stationId: Int,
        val stationName: String,
        val lineId: Int?,
        val announcementStationId: Int? = stationId,
    )

    companion object {
        private const val MAX_QUEUED_STATION_EVENTS = 8

        private var instance: MetroClientStorage? = null

        fun getInstance(): MetroClientStorage {
            if (instance == null) {
                instance = MetroClientStorage()
            }
            return instance!!
        }
    }
}
