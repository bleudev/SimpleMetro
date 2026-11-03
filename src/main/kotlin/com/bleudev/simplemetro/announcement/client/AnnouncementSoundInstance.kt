package com.bleudev.simplemetro.announcement.client

import com.bleudev.simplemetro.SMetro
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance
import net.minecraft.client.resources.sounds.AbstractSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.AudioStream
import net.minecraft.client.sounds.SoundBufferLibrary
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Util
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

@Environment(EnvType.CLIENT)
internal class AnnouncementSoundInstance(private val wavPaths: List<Path>) : AbstractSoundInstance(
    SMetro.resolve("station_announcement"),
    SoundSource.RECORDS,
    SoundInstance.createUnseededRandom(),
), FabricSoundInstance {
    init {
        volume = 1.0f
        pitch = 1.0f
        relative = true
        attenuation = SoundInstance.Attenuation.NONE
    }

    override fun getAudioStream(
        library: SoundBufferLibrary,
        id: Identifier,
        repeatInstantly: Boolean,
    ): CompletableFuture<AudioStream> = CompletableFuture.supplyAsync(
        { WavAudioStream(wavPaths) },
        Util.nonCriticalIoPool(),
    )
}
