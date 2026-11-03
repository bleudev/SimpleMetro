package com.bleudev.simplemetro.announcement.client

import net.minecraft.client.sounds.AudioStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

internal class WavAudioStream(paths: List<Path>) : AudioStream {
    private val pathIterator = paths.iterator()
    private var stream: AudioInputStream? = null
    private var channels = 0
    private val format = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48_000f, 16, 2, 4, 48_000f, false)

    init {
        if (!openNext()) throw IOException("No announcement WAV files were provided")
    }

    private fun openNext(): Boolean {
        while (pathIterator.hasNext()) {
            val source = AudioSystem.getAudioInputStream(pathIterator.next().toFile())
            val sourceChannels = source.format.channels
            if (sourceChannels !in 1..2) {
                source.close()
                throw IOException("Announcement WAV must be mono or stereo (got $sourceChannels channels)")
            }

            val target = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                48_000f,
                16,
                sourceChannels,
                sourceChannels * 2,
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
            channels = sourceChannels
            return true
        }
        return false
    }

    override fun getFormat(): AudioFormat = format

    override fun read(expectedSize: Int): ByteBuffer {
        val frameSize = format.frameSize
        val alignedSize = expectedSize - expectedSize % frameSize
        if (alignedSize <= 0) return ByteBuffer.allocate(0)

        val output = ByteArray(alignedSize)
        var outputOffset = 0
        while (outputOffset < output.size) {
            if (stream == null && !openNext()) break
            val current = stream ?: break
            val channelCount = channels
            val inputFrameSize = channelCount * 2
            val input = ByteArray((output.size - outputOffset) / frameSize * inputFrameSize)
            var inputOffset = 0
            while (inputOffset < input.size) {
                val count = current.read(input, inputOffset, input.size - inputOffset)
                if (count <= 0) break
                inputOffset += count
            }

            val usableSize = inputOffset - inputOffset % inputFrameSize
            if (usableSize > 0) {
                val samples = ByteBuffer.wrap(input, 0, usableSize).order(ByteOrder.LITTLE_ENDIAN)
                repeat(usableSize / inputFrameSize) {
                    val left = samples.short.toInt()
                    val right = if (channelCount == 2) samples.short.toInt() else left
                    output[outputOffset++] = left.toByte()
                    output[outputOffset++] = (left shr 8).toByte()
                    output[outputOffset++] = right.toByte()
                    output[outputOffset++] = (right shr 8).toByte()
                }
            }

            if (inputOffset < input.size) {
                current.close()
                stream = null
                channels = 0
            }
        }

        if (outputOffset == 0) return ByteBuffer.allocate(0)
        val result = ByteBuffer.allocateDirect(outputOffset)
        result.put(output, 0, outputOffset)
        result.flip()
        return result
    }

    override fun close() {
        stream?.close()
        stream = null
    }
}
