package com.bleudev.simplemetro.client.storage

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import java.util.*

@Environment(EnvType.CLIENT)
class MetroClientStorage private constructor() {
    /** Station the player is currently standing at. Invisible waypoints are never stored here. */
    var currentStation: Int? = null

    /** Last visible station passed; while travelling this is the origin of the active track segment. */
    var prevVisibleStation: Int? = null

    /** Next visible station on the selected line, after any invisible geometry waypoints. */
    var nextVisibleStation: Int? = null

    var currentLine: Int? = null
    var stationProgress: Float? = null

    private val stationEvents = ArrayDeque<StationEvent>()
    private val soundEvents = ArrayDeque<StationEvent>()
    private var nextEventId = 0L

    fun onPlayerTick(player: LocalPlayer, level: ClientLevel) {
        LineTableExtractor.getInstance().playerTick(this, player, level)
    }

    fun publishStationEvent(type: StationEventType, stationId: Int, stationName: String) {
        if (stationEvents.size >= MAX_QUEUED_STATION_EVENTS) stationEvents.removeFirst()
        if (soundEvents.size >= MAX_QUEUED_STATION_EVENTS) soundEvents.removeFirst()
        val event = StationEvent(++nextEventId, type, stationId, stationName)
        stationEvents.addLast(event)
        soundEvents.addLast(event)
    }

    fun pollStationEvent(): StationEvent? = stationEvents.pollFirst()
    fun pollSoundEvent(): StationEvent? = soundEvents.pollFirst()

    enum class StationEventType {
        ARRIVAL,
        DEPARTURE,
    }

    data class StationEvent(
        val id: Long,
        val type: StationEventType,
        val stationId: Int,
        val stationName: String,
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
