package com.bleudev.simplemetro.announcement

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.Announcements
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ClientboundStationAnnouncement
import com.bleudev.simplemetro.util.sendPacket
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import java.util.*

object ServerAnnouncementTracker {
    private data class PlayerState(
        val dimension: Identifier,
        val stationId: Int?,
        val lastPosition: Vec3,
        val lineId: Int?,
        val direction: Int?,
        val nextStationId: Int?,
    )

    private val states = HashMap<UUID, PlayerState>()
    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        ServerLifecycleEvents.SERVER_STOPPED.register {
            states.clear()
        }
        ServerTickEvents.END_SERVER_TICK.register(::tick)
    }

    private fun tick(server: MinecraftServer) {
        val onlineIds = server.playerList.players.mapTo(hashSetOf()) { it.uuid }
        states.keys.removeIf { it !in onlineIds }

        for (player in server.playerList.players) {
            try { updatePlayer(player) }
            catch (exception: Exception) {
                SMetro.LOGGER.error(
                    "Failed to update station announcements for {}",
                    player.uuid,
                    exception
                )
            }
        }
    }

    private fun updatePlayer(player: ServerPlayer) {
        val dimension = player.level().dimension().identifier()
        val info = SMetroInfo.getInstance(dimension)
        val position = player.position()
        val previous = states[player.uuid]

        if (previous == null || previous.dimension != dimension) {
            val current = StationRouteResolver.detectVisibleStation(info.stations, null, position)
            val route = current?.let {
                StationRouteResolver.resolveNextStation(info, it, position, Vec3.ZERO)
            }
            states[player.uuid] = PlayerState(
                dimension,
                current,
                position,
                route?.lineId,
                route?.direction,
                route?.nextStationId,
            )
            if (current != null) {
                val lineId = states[player.uuid]?.lineId
                tryBroadcast(player, info, dimension, current, lineId, Announcements.AnnouncementType.ARRIVAL)
            }
            return
        }

        val motion = position.subtract(previous.lastPosition)
        val current = StationRouteResolver.detectVisibleStation(info.stations, previous.stationId, position)

        if (previous.stationId != current) {
            when {
                previous.stationId != null && current == null -> {
                    val route = StationRouteResolver.resolveNextStation(
                        info,
                        previous.stationId,
                        position,
                        motion,
                        previous.lineId,
                        previous.direction,
                    )
                    val lineId = route?.lineId ?: previous.lineId
                    val targetStationId = route?.nextStationId ?: previous.nextStationId
                    targetStationId?.let { nextId ->
                        tryBroadcast(player, info, dimension, nextId, lineId, Announcements.AnnouncementType.DEPARTURE)
                    }

                    states[player.uuid] = PlayerState(
                        dimension,
                        null,
                        position,
                        lineId,
                        route?.direction ?: previous.direction,
                        route?.nextStationId ?: previous.nextStationId,
                    )
                    return
                }

                current != null -> {
                    val lineId = states[player.uuid]?.lineId
                    tryBroadcast(player, info, dimension, current, lineId, Announcements.AnnouncementType.ARRIVAL)
                }
            }
        }

        val route = current?.let {
            StationRouteResolver.resolveNextStation(info, it, position, motion, previous.lineId, previous.direction)
        }
        states[player.uuid] = PlayerState(
            dimension,
            current,
            position,
            route?.lineId ?: previous.lineId,
            route?.direction ?: previous.direction,
            route?.nextStationId ?: previous.nextStationId,
        )
    }

    private fun tryBroadcast(player: ServerPlayer, info: SMetroInfo, dimension: Identifier, stationId: Int, lineId: Int?, type: Announcements.AnnouncementType) {
        var id = info.stations.firstOrNull { it.id == stationId }?.announcements?.get(type)
        if (id == null && lineId != null) {
            id = info.lines.firstOrNull { it.id == lineId }?.announcements?.get(type)
        }

        if (id != null) {
            broadcast(player, dimension, id)
        }
    }

    private fun broadcast(player: ServerPlayer, dimension: Identifier, announcementId: String) {
        if (!AnnouncementId.isValid(announcementId)) return
        if (AnnouncementTransportState.playWithSVC(player, announcementId)) return
        player.sendPacket(ClientboundStationAnnouncement(dimension, announcementId))
    }
}
