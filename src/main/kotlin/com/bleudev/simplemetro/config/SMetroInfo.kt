package com.bleudev.simplemetro.config

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.client.storage.MetroSchemeExtractor
import com.bleudev.simplemetro.config.obj.MetroLine
import com.bleudev.simplemetro.config.obj.MetroStation
import com.bleudev.simplemetro.custom.network.protocol.common.custom.ServerboundMetroInfoEdit
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
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

@Serializable
data class SMetroInfo(val stations: List<MetroStation> = listOf(), val lines: List<MetroLine> = listOf()) {
    companion object {
        private val JSON = Json { encodeDefaults = true; ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

        private var basePath: Path = FabricLoader.getInstance().configDir.resolve(SMetro.ID)
        private fun pathOf(dimension: Identifier): Path = basePath.resolve(dimension.namespace).resolve("${dimension.path}.json")

        private var instance: SMetroInfo? = null
        private var instanceRaw: String? = null
        private var syncedDimensionMap: Map<Identifier, SMetroInfo> = mapOf()

        private fun load(dim: Identifier, s: String): SMetroInfo {
            var s = s
            if (!Files.exists(pathOf(dim))) {
                Files.createDirectories(pathOf(dim).parent)
                s = JSON.encodeToString(SMetroInfo())
                Files.writeString(pathOf(dim), s)
            }
            return JSON.decodeFromString(s)
        }

        private fun load(dim: Identifier): SMetroInfo {
            val raw = if (Files.exists(pathOf(dim))) Files.readString(pathOf(dim)) else ""
            if (instance == null || instanceRaw == null || raw != instanceRaw) {
                instance = load(dim, raw)
                instanceRaw = raw
                if (isClientSide) MetroSchemeExtractor.rebuild(dim, instance!!)
            }
            return instance!!
        }

        private fun collectDimensions(): List<Identifier> {
            Files.createDirectories(basePath)
            return Files.walk(basePath).filter {
                val f = it.toFile()
                return@filter f.isFile && f.extension == "json"
            }.toList().mapNotNull { if (it.parent != basePath) Identifier.fromNamespaceAndPath(it.parent.name, it.nameWithoutExtension) else null }.toList()
        }

        private fun internalSave(dim: Identifier, info: SMetroInfo) {
            if (!Files.exists(pathOf(dim))) {
                Files.createDirectories(pathOf(dim).parent)
            }
            Files.writeString(pathOf(dim), JSON.encodeToString(info))
            if (isClientSide) MetroSchemeExtractor.rebuild(dim, info)
        }

        fun getInstance(dimension: Identifier): SMetroInfo {
            return if (isClientSide && idModOnServer) {
                syncedDimensionMap[dimension] ?: SMetroInfo()
            } else {
                load(dimension)
            }
        }

        fun getInstances(): Map<Identifier, SMetroInfo> {
            return if (isClientSide && idModOnServer) syncedDimensionMap
            else collectDimensions().associateWith { load(it) }
        }

        fun save(dimension: Identifier, info: SMetroInfo) {
            if (isClientSide && idModOnServer) {
                ClientPlayNetworking.send(ServerboundMetroInfoEdit(dimension, info))
            } else {
                internalSave(dimension, info)
            }
        }

        @Environment(EnvType.CLIENT)
        fun endSync(inst: Map<Identifier, SMetroInfo>) {
            syncedDimensionMap = inst
            MetroSchemeExtractor.rebuild(inst)
        }

        fun serverInit(server: MinecraftServer) {
            basePath = server.getWorldPath(LevelResource(SMetro.ID))
        }

        @Environment(EnvType.CLIENT)
        fun clientInit(path: Path) {
            basePath = path
            Files.createDirectories(basePath)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SMetroInfo> = StreamCodec.composite(
            ByteBufCodecs.list<RegistryFriendlyByteBuf, MetroStation>().apply(MetroStation.STREAM_CODEC), SMetroInfo::stations,
            ByteBufCodecs.list<RegistryFriendlyByteBuf, MetroLine>().apply(MetroLine.STREAM_CODEC), SMetroInfo::lines,
            ::SMetroInfo
        )
    }
}