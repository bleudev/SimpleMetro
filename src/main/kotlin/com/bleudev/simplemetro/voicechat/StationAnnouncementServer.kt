package com.bleudev.simplemetro.voicechat

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.announcement.AnnouncementId
import com.bleudev.simplemetro.announcement.AnnouncementTransportState
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.MetroStation
import de.maxhenkel.voicechat.api.VoicechatConnection
import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

internal object StationAnnouncementServer {
    private const val ANNOUNCEMENTS_CATEGORY = "announcements"
    private const val STATION_ENTER_RADIUS = 7.0
    private const val STATION_EXIT_RADIUS = 9.0
    private const val STATION_SWITCH_MARGIN = 1.0
    private const val STATION_CHECK_HEIGHT = 5.0
    private const val MAX_WAV_BYTES = 32 * 1024 * 1024

    private data class PlayerState(
        val dimension: Identifier,
        val stationId: Int?,
    )

    private data class CachedWave(
        val modifiedAt: Long,
        val size: Long,
        val samples: ShortArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CachedWave

            if (modifiedAt != other.modifiedAt) return false
            if (size != other.size) return false
            if (!samples.contentEquals(other.samples)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = modifiedAt.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + samples.contentHashCode()
            return result
        }
    }

    private val playerStates = HashMap<UUID, PlayerState>()
    private val waveCache = ConcurrentHashMap<Path, CachedWave>()
    private var api: VoicechatServerApi? = null
    private var started = false
    private var initializedServer: MinecraftServer? = null

    fun start(serverApi: VoicechatServerApi) {
        api = serverApi
        AnnouncementTransportState.setVoicechatAvailable(true)
        if (started) return
        started = true

        ServerTickEvents.START_SERVER_TICK.register(::updateVoicechatPlayers)
        ServerTickEvents.END_SERVER_TICK.register(::tick)
        ServerLifecycleEvents.SERVER_STOPPED.register {
            playerStates.clear()
            waveCache.clear()
            api = null
            initializedServer = null
            AnnouncementTransportState.reset()
        }
    }

    private fun updateVoicechatPlayers(server: MinecraftServer) {
        val voicechat = api ?: return
        if (initializedServer != server) {
            SMetroInfo.serverInit(server)
            initializedServer = server
            playerStates.clear()
            waveCache.clear()
        }

        val onlinePlayers = server.playerList.players
        val connectedPlayers = onlinePlayers.mapNotNull { player ->
            runCatching<VoicechatConnection?> { voicechat.getConnectionOf(player.gameProfile.id) }
            .onFailure { exception ->
                SMetro.LOGGER.debug("Could not query Simple Voice Chat connection for {}", player.uuid, exception)
            }
            .getOrNull()
            ?.let { player.uuid }
        }
        AnnouncementTransportState.updateVoicechatPlayers(connectedPlayers)
    }

    private fun tick(server: MinecraftServer) {
        val voicechat = api ?: return
        val onlinePlayers = server.playerList.players
        val onlineIds = onlinePlayers.mapTo(hashSetOf()) { it.uuid }
        playerStates.keys.removeIf { it !in onlineIds }

        for (player in onlinePlayers) {
            try {
                updatePlayer(voicechat, player)
            } catch (exception: Exception) {
                SMetro.LOGGER.error("Failed to update station announcements for {}", player.uuid, exception)
            }
        }
    }

    private fun updatePlayer(voicechat: VoicechatServerApi, player: ServerPlayer) {
        val dimension = player.level().dimension().identifier()
        val info = SMetroInfo.getInstance(dimension)
        val previousState = playerStates[player.uuid]

        if (previousState == null || previousState.dimension != dimension) {
            val current = detectVisibleStation(info.stations, null, player.position())
            playerStates[player.uuid] = PlayerState(dimension, current)
            if (current != null) playStationAnnouncement(voicechat, player, info.stations, current, true)
            return
        }

        val current = detectVisibleStation(info.stations, previousState.stationId, player.position())
        if (current == previousState.stationId) return

        previousState.stationId?.let { oldStationId ->
            playStationAnnouncement(voicechat, player, info.stations, oldStationId, false)
        }
        current?.let { newStationId ->
            playStationAnnouncement(voicechat, player, info.stations, newStationId, true)
        }
        playerStates[player.uuid] = PlayerState(dimension, current)
    }

    /** Uses the same radii and hysteresis as the client-side LinePanelExtractor. */
    private fun detectVisibleStation(
        stations: List<MetroStation>,
        previousCurrent: Int?,
        playerPos: Vec3,
    ): Int? {
        var bestId: Int? = null
        var bestDistance = Double.POSITIVE_INFINITY

        for (station in stations) {
            if (station.status.invisible) continue
            val stationPos = Vec3.atCenterOf(station.pos)
            val horizontalDistance = stationPos.horizontal().distanceTo(playerPos.horizontal())
            val verticalDistance = kotlin.math.abs(stationPos.y - playerPos.y)
            if (horizontalDistance <= STATION_ENTER_RADIUS &&
                verticalDistance <= STATION_CHECK_HEIGHT &&
                horizontalDistance < bestDistance
            ) {
                bestId = station.id
                bestDistance = horizontalDistance
            }
        }

        if (previousCurrent != null) {
            val previous = stations.firstOrNull { it.id == previousCurrent }
            if (previous != null && !previous.status.invisible) {
                val stationPos = Vec3.atCenterOf(previous.pos)
                val oldDistance = stationPos.horizontal().distanceTo(playerPos.horizontal())
                val oldHeight = kotlin.math.abs(stationPos.y - playerPos.y)
                if (oldDistance <= STATION_EXIT_RADIUS && oldHeight <= STATION_CHECK_HEIGHT &&
                    (bestId == null || bestId == previousCurrent || bestDistance + STATION_SWITCH_MARGIN >= oldDistance)
                ) {
                    return previousCurrent
                }
            }
        }

        return bestId
    }

    private fun playStationAnnouncement(
        voicechat: VoicechatServerApi,
        player: ServerPlayer,
        stations: List<MetroStation>,
        stationId: Int,
        arrival: Boolean,
    ) {
        val station = stations.firstOrNull { it.id == stationId } ?: return
        val announcementId = if (arrival) station.arrivalAnnouncement else station.departureAnnouncement
        if (!AnnouncementId.isValid(announcementId)) return

        val audioFile = SMetroInfo.announcementsDirectory
            .resolve("${announcementId}.wav")
            .normalize()
        if (audioFile.parent != SMetroInfo.announcementsDirectory.normalize() || !Files.isRegularFile(audioFile)) {
            return
        }

        val connection = voicechat.getConnectionOf(player.gameProfile.id) ?: return
        val samples = try {
            loadWave(audioFile)
        } catch (exception: Exception) {
            SMetro.LOGGER.warn("Could not load station announcement WAV '{}'", audioFile, exception)
            return
        }

        val channel: StaticAudioChannel = voicechat.createStaticAudioChannel(UUID.randomUUID()) ?: return
        channel.addTarget(connection)
        channel.category = ANNOUNCEMENTS_CATEGORY

        val encoder = try {
            voicechat.createEncoder()
        } catch (exception: Exception) {
            channel.clearTargets()
            SMetro.LOGGER.warn("Could not create Simple Voice Chat encoder", exception)
            return
        }

        try {
            val audioPlayer = voicechat.createAudioPlayer(channel, encoder, samples)
            audioPlayer.setOnStopped {
                runCatching { encoder.close() }
                channel.clearTargets()
            }
            audioPlayer.startPlaying()
        } catch (exception: Exception) {
            runCatching { encoder.close() }
            channel.clearTargets()
            SMetro.LOGGER.warn("Could not play station announcement '{}'", announcementId, exception)
        }
    }

    private fun loadWave(path: Path): ShortArray {
        val modifiedAt = Files.getLastModifiedTime(path).toMillis()
        val size = Files.size(path)
        if (size <= 0L || size > MAX_WAV_BYTES) {
            throw IOException("WAV file has an invalid size: $size bytes")
        }

        waveCache[path]?.let { cached ->
            if (cached.modifiedAt == modifiedAt && cached.size == size) return cached.samples
        }

        val samples = AudioSystem.getAudioInputStream(path.toFile()).use { source ->
            val channels = source.format.channels
            if (channels !in 1..8) {
                throw IOException("Unsupported WAV channel count: $channels")
            }
            val frameSize = channels * 2
            val targetFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                48_000f,
                16,
                channels,
                frameSize,
                48_000f,
                false,
            )
            AudioSystem.getAudioInputStream(targetFormat, source).use { converted ->
                val bytes = converted.readAllBytes()
                val usableBytes = bytes.size - (bytes.size % frameSize)
                if (usableBytes == 0) throw IOException("WAV file contains no audio samples")

                val interleaved = ShortArray(usableBytes / 2)
                ByteBuffer.wrap(bytes, 0, usableBytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(interleaved)

                if (channels == 1) {
                    interleaved
                } else {
                    ShortArray(interleaved.size / channels) { frame ->
                        var sum = 0L
                        for (channel in 0 until channels) {
                            sum += interleaved[frame * channels + channel].toLong()
                        }
                        (sum / channels).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
            }
        }
        waveCache[path] = CachedWave(modifiedAt, size, samples)
        return samples
    }
}
