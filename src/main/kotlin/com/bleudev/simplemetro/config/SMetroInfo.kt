package com.bleudev.simplemetro.config

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.client.util.MetroSchemeRenderer
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ServerboundMetroInfoEdit
import com.bleudev.simplemetro.obj.MetroLine
import com.bleudev.simplemetro.obj.MetroStation
import com.bleudev.simplemetro.util.idModOnServer
import com.bleudev.simplemetro.util.isClientSide
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.nio.file.Files

@Serializable
data class SMetroInfo(val stations: List<MetroStation> = listOf(), val lines: List<MetroLine> = listOf()) {
    companion object {
        private val JSON = Json { encodeDefaults = true; ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }
        private val path get() = FabricLoader.getInstance().configDir.resolve(SMetro.MOD_ID).resolve("scheme.info.json")

        private var instance: SMetroInfo? = null
        private var instanceRaw: String? = null
        private var syncedInstance: SMetroInfo? = null

        private fun load(s: String): SMetroInfo {
            if (!Files.exists(path)) {
                Files.createDirectories(path.parent)
                Files.writeString(path, Json.encodeToString(SMetroInfo()))
            }

            return JSON.decodeFromString(s)
        }

        private fun load(): SMetroInfo {
            val raw = if (Files.exists(path)) Files.readString(path) else ""
            if (instance == null || instanceRaw == null || raw != instanceRaw) {
                instance = load(raw)
                instanceRaw = raw
                if (isClientSide) MetroSchemeRenderer.getInstance().onUpdate()
            }
            return instance!!
        }

        private fun internalSave(info: SMetroInfo) {
            if (!Files.exists(path)) {
                Files.createDirectories(path.parent)
            }
            Files.writeString(path, JSON.encodeToString(info))
            if (isClientSide) MetroSchemeRenderer.getInstance().onUpdate()
        }

        fun getInstance(): SMetroInfo {
            return if (isClientSide) {
                if (idModOnServer) syncedInstance ?: SMetroInfo() else load()
            } else {
                load()
            }
        }

        fun save(info: SMetroInfo) {
            if (isClientSide) {
                if (idModOnServer) ClientPlayNetworking.send(ServerboundMetroInfoEdit(info)) else internalSave(info)
            } else {
                internalSave(info)
            }
        }

        @Environment(EnvType.CLIENT)
        fun endSync(inst: SMetroInfo) {
            syncedInstance = inst
            MetroSchemeRenderer.getInstance().onUpdate()
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SMetroInfo> = StreamCodec.composite(
            ByteBufCodecs.list<RegistryFriendlyByteBuf, MetroStation>().apply(MetroStation.STREAM_CODEC), SMetroInfo::stations,
            ByteBufCodecs.list<RegistryFriendlyByteBuf, MetroLine>().apply(MetroLine.STREAM_CODEC), SMetroInfo::lines,
            ::SMetroInfo
        )
    }
}