package com.bleudev.simplemetro.compat.svc

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.compat.SVCCompat
import com.bleudev.simplemetro.config.SMetroInfo
import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel
import net.minecraft.server.level.ServerPlayer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class SVCCompatImpl(private val serverApi: VoicechatServerApi) : SVCCompat {
    override fun handleAnnouncements(player: ServerPlayer, ids: List<String>): Boolean = playByIds(serverApi, player, ids)

    override fun flushAnnouncementCache() {
        activePlayers.values.forEach { runCatching { it.stopPlaying() } }
        activePlayers.clear()
        waveCache.clear()
    }

    companion object {
        private const val ANNOUNCEMENTS_CATEGORY = "announcements"
        private const val MAX_WAV_BYTES = 32 * 1024 * 1024
    }

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

    private val waveCache = ConcurrentHashMap<Path, CachedWave>()
    private val activePlayers = ConcurrentHashMap<UUID, AudioPlayer>()

    private fun playByIds(
        voicechat: VoicechatServerApi,
        player: ServerPlayer,
        announcementIds: List<String>,
    ): Boolean {
        if (announcementIds.isEmpty()) return false

        val directory = SMetroInfo.announcementsDirectory.toAbsolutePath().normalize()
        val parts = ArrayList<ShortArray>(announcementIds.size)
        for (announcementId in announcementIds) {
            val audioFile = directory.resolve("$announcementId.wav").normalize()
            if (audioFile.parent != directory || !Files.isRegularFile(audioFile)) return false
            try {
                parts.add(loadWave(audioFile))
            } catch (exception: Exception) {
                SMetro.LOGGER.warn("Could not load station announcement WAV '{}'", audioFile, exception)
                return false
            }
        }

        val sampleCount = parts.sumOf { it.size }
        if (sampleCount == 0) return false
        val samples = ShortArray(sampleCount)
        var sampleOffset = 0
        for (part in parts) {
            part.copyInto(samples, sampleOffset)
            sampleOffset += part.size
        }

        val connection = voicechat.getConnectionOf(player.gameProfile.id) ?: return false
        val channel: StaticAudioChannel = voicechat.createStaticAudioChannel(UUID.randomUUID()) ?: return false
        channel.addTarget(connection)
        channel.category = ANNOUNCEMENTS_CATEGORY

        val encoder = try {
            voicechat.createEncoder()
        } catch (exception: Exception) {
            channel.clearTargets()
            SMetro.LOGGER.warn("Could not create Simple Voice Chat encoder", exception)
            return false
        }

        var startedPlayer: AudioPlayer? = null
        return try {
            val audioPlayer = voicechat.createAudioPlayer(channel, encoder, samples)
            startedPlayer = audioPlayer
            audioPlayer.setOnStopped {
                activePlayers.remove(player.uuid, audioPlayer)
                runCatching { encoder.close() }
                channel.clearTargets()
            }
            activePlayers.put(player.uuid, audioPlayer)?.let { previous ->
                runCatching { previous.stopPlaying() }
            }
            audioPlayer.startPlaying()
            true
        } catch (exception: Exception) {
            SMetro.LOGGER.warn("Could not play station announcement sequence {}", announcementIds, exception)
            startedPlayer?.let {
                activePlayers.remove(player.uuid, it)
                runCatching { it.stopPlaying() }
            }
            runCatching { encoder.close() }
            channel.clearTargets()
            false
        }
    }

    private fun loadWave(path: Path): ShortArray {
        val modifiedAt = Files.getLastModifiedTime(path).toMillis()
        val size = Files.size(path)
        if (size !in 1..MAX_WAV_BYTES) {
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