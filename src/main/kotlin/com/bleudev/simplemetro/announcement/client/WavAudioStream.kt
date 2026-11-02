package com.bleudev.simplemetro.announcement.client

import net.minecraft.client.sounds.AudioStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Path
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

internal class WavAudioStream(path: Path) : AudioStream {
    private val stream: AudioInputStream
    private val format: AudioFormat

    init {
        val source = AudioSystem.getAudioInputStream(path.toFile())
        val channels = source.format.channels
        if (channels !in 1..2) {
            source.close()
            throw IOException("Announcement WAV must be mono or stereo (got $channels channels)")
        }

        val target = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            48_000f,
            16,
            channels,
            channels * 2,
            48_000f,
            false,
        )
        stream = if (source.format.matches(target)) source else {
            try {
                AudioSystem.getAudioInputStream(target, source)
            } catch (exception: Exception) {
                source.close()
                throw IOException("Unsupported WAV format: ${source.format}", exception)
            }
        }
        format = stream.format
    }

    override fun getFormat(): AudioFormat = format

    override fun read(expectedSize: Int): ByteBuffer {
        val frameSize = format.frameSize
        val alignedSize = expectedSize - expectedSize % frameSize
        if (alignedSize <= 0) return ByteBuffer.allocate(0)

        val bytes = ByteArray(alignedSize)
        var offset = 0
        while (offset < bytes.size) {
            val count = stream.read(bytes, offset, bytes.size - offset)
            if (count < 0) break
            if (count == 0) break
            offset += count
        }

        val usableSize = offset - offset % frameSize
        if (usableSize <= 0) return ByteBuffer.allocate(0)

        val result = ByteBuffer.allocateDirect(usableSize)
        result.put(bytes, 0, usableSize)
        result.flip()
        return result
    }

    override fun close() {
        stream.close()
    }
}
