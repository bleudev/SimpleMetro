package com.bleudev.simplemetro.announcement

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.client.storage.MetroClientStorage
import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.util.idModOnServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.repository.PackRepository
import net.minecraft.sounds.SoundEvent
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Environment(EnvType.CLIENT)
object StationAnnouncementClient {
    private const val PACK_ID = "simplemetro_station_announcements"
    private const val SOUNDS_DIRECTORY = "sounds"
    private const val MAX_PENDING_EVENTS = 8

    private val packDirectory: Path = FabricLoader.getInstance().configDir
        .resolve(SMetro.ID)
        .resolve("announcement_resourcepack")
    private val json = Json { prettyPrint = true }
    private val pendingEvents = ArrayDeque<Pair<Identifier, MetroClientStorage.StationEvent>>()

    private data class StationSounds(
        val arrival: Identifier?,
        val departure: Identifier?,
    )

    private data class SoundEntry(
        val eventPath: String,
        val resourcePath: String,
        val source: Path,
    )

    private var repository: PackRepository? = null
    private var finderRegistered = false
    private var resourcesReady = false
    private var reloadGeneration = 0
    private var lastFingerprint: String? = null
    private var serverVoicechatAvailable: Boolean? = null
    private var stationSounds: Map<Pair<Identifier, Int>, StationSounds> = emptyMap()

    fun initialize() {
        if (finderRegistered) return

        val mc = Minecraft.getInstance()
//        mc.reloadResourcePacks()
        val repo = mc.resourcePackRepository
//        repo.addPack(RepositorySource { output ->
//            if (Files.isRegularFile(packDirectory.resolve("pack.mcmeta"))) {
//                val location = PackLocationInfo(
//                    PACK_ID,
//                    Component.literal("SimpleMetro announcements"),
//                    PackSource.BUILT_IN,
//                    Optional.empty(),
//                )
//                val pack = Pack.readMetaAndCreate(
//                    location,
//                    PathPackResources.PathResourcesSupplier(packDirectory),
//                    PackType.CLIENT_RESOURCES,
//                    PackSelectionConfig(false, Pack.Position.TOP, false),
//                )
//                if (pack != null) output.accept(pack)
//            }
//        })
        repository = repo
        finderRegistered = true
    }

    fun setServerVoicechatAvailable(available: Boolean) {
        serverVoicechatAvailable = available
        if (available) pendingEvents.clear() else playQueuedEvents()
    }

    fun update(dimensions: Map<Identifier, SMetroInfo>) {
        initialize()
        val entries = linkedMapOf<String, SoundEntry>()
        val stationSounds = hashMapOf<Pair<Identifier, Int>, StationSounds>()
        val fingerprintParts = mutableListOf<String>()

        for ((dimension, info) in dimensions.toSortedMap(compareBy { it.toString() })) {
            val soundDirectory = SMetroInfo.announcementsDirectory
            for (station in info.stations.sortedBy { it.id }) {
                fun resolveSound(soundId: String?): Identifier? {
                    if (!AnnouncementId.isValid(soundId)) return null
                    val id = soundId!!
                    val normalizedDirectory = soundDirectory.normalize()
                    val source = normalizedDirectory.resolve("$id.ogg").normalize()
                    if (source.parent != normalizedDirectory) return null
                    if (!Files.isRegularFile(source)) {
                        fingerprintParts.add("$dimension:$id:missing")
                        return null
                    }

                    val attributes = runCatching {
                        Files.readAttributes(source, java.nio.file.attribute.BasicFileAttributes::class.java)
                    }.getOrNull() ?: run {
                        fingerprintParts.add("$dimension:$id:unreadable")
                        return null
                    }
                    fingerprintParts.add("$dimension:$id:${attributes.size()}:${attributes.lastModifiedTime().toMillis()}")

                    val eventPath = "announcement/${dimension.namespace}/${dimension.path}/$id"
                    entries.putIfAbsent(eventPath, SoundEntry(eventPath, eventPath, source))
                    return SMetro.resolve(eventPath)
                }

                val arrival = resolveSound(station.arrivalAnnouncement)
                val departure = resolveSound(station.departureAnnouncement)
                if (arrival != null || departure != null) {
                    stationSounds[dimension to station.id] = StationSounds(arrival, departure)
                }
            }
        }

        val fingerprint = fingerprintParts.sorted().joinToString("|")
        this.stationSounds = stationSounds
        if (fingerprint == lastFingerprint) return
        lastFingerprint = fingerprint
        resourcesReady = false

        try {
            writeResourcePack(entries.values.toList())
            reloadPack(entries.isNotEmpty())
        } catch (exception: Exception) {
            SMetro.LOGGER.error("Failed to prepare station announcement sounds", exception)
            resourcesReady = true
            playQueuedEvents()
        }
    }

    fun play(dimension: Identifier, event: MetroClientStorage.StationEvent) {
        when (shouldPlayLocally()) {
            false -> return
            null -> {
                queueEvent(dimension, event)
                return
            }
            true -> Unit
        }

        if (!resourcesReady) {
            queueEvent(dimension, event)
            return
        }
        playNow(dimension, event)
    }

    fun onDisconnect() {
        serverVoicechatAvailable = null
        pendingEvents.clear()
        stationSounds = emptyMap()
        update(emptyMap())
    }

    private fun writeResourcePack(entries: List<SoundEntry>) {
        deleteDirectoryContents(packDirectory)
        val assets = packDirectory.resolve("assets").resolve(SMetro.ID)
        val soundsDirectory = assets.resolve(SOUNDS_DIRECTORY)
        Files.createDirectories(soundsDirectory)

        Files.writeString(
            packDirectory.resolve("pack.mcmeta"),
            """{"pack":{"description":"SimpleMetro station announcements","min_format":[97,1],"max_format":[97,1]}}""",
        )

        val soundsJson = buildJsonObject {
            for (entry in entries) {
                put(entry.eventPath, buildJsonObject {
                    put("sounds", buildJsonArray {
                        add(buildJsonObject {
                            put("name", "${SMetro.ID}:${entry.resourcePath}")
                        })
                    })
                })
                val destination = soundsDirectory.resolve("${entry.resourcePath}.ogg").normalize()
                if (!destination.startsWith(soundsDirectory)) {
                    throw IllegalArgumentException("Invalid announcement resource path: ${entry.resourcePath}")
                }
                Files.createDirectories(destination.parent)
                Files.copy(entry.source, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        Files.writeString(assets.resolve("sounds.json"), json.encodeToString(soundsJson))
    }

    private fun deleteDirectoryContents(directory: Path) {
        if (!Files.exists(directory)) return
        Files.walk(directory).use { paths ->
            paths.sorted(Comparator.reverseOrder<Path>())
                .filter { it != directory }
                .forEach { Files.deleteIfExists(it) }
        }
    }

    private fun reloadPack(shouldEnable: Boolean) {
        val mc = Minecraft.getInstance()
        val repo = repository ?: return
        repo.reload()

        val selected = repo.getSelectedIds().toMutableList()
        if (shouldEnable && PACK_ID in repo.getAvailableIds()) {
            if (PACK_ID !in selected) selected.add(PACK_ID)
        } else {
            selected.removeAll { it == PACK_ID }
        }
        repo.setSelected(selected)

        val generation = ++reloadGeneration
        mc.reloadResourcePacks().whenComplete { _, failure ->
            mc.execute {
                if (generation != reloadGeneration) return@execute
                if (failure != null) {
                    SMetro.LOGGER.error("Failed to reload station announcement resource pack", failure)
                }
                resourcesReady = true
                playQueuedEvents()
            }
        }
    }

    private fun queueEvent(dimension: Identifier, event: MetroClientStorage.StationEvent) {
        if (pendingEvents.size >= MAX_PENDING_EVENTS) pendingEvents.removeFirst()
        pendingEvents.addLast(dimension to event)
    }

    private fun playQueuedEvents() {
        while (pendingEvents.isNotEmpty()) {
            val shouldPlay = shouldPlayLocally() ?: return
            if (!resourcesReady) return
            val (dimension, event) = pendingEvents.removeFirst()
            if (shouldPlay) playNow(dimension, event)
        }
    }

    /**
     * Returns null while a remote SimpleMetro server has not sent its first transport status.
     * This avoids playing locally just before the server sends the same event through Voice Chat.
     */
    private fun shouldPlayLocally(): Boolean? {
        val mc = Minecraft.getInstance()
        val playerId = mc.player?.uuid
        if (mc.singleplayerServer != null) {
            return playerId == null || !AnnouncementTransportState.canPlayFor(playerId)
        }
        if (idModOnServer && serverVoicechatAvailable == null) return null
        return serverVoicechatAvailable != true
    }

    private fun playNow(dimension: Identifier, event: MetroClientStorage.StationEvent) {
        val sounds = stationSounds[dimension to event.stationId] ?: return
        val soundId = when (event.type) {
            MetroClientStorage.StationEventType.ARRIVAL -> sounds.arrival
            MetroClientStorage.StationEventType.DEPARTURE -> sounds.departure
        } ?: return

        try {
            val soundEvent = SoundEvent.createVariableRangeEvent(soundId)
            Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(soundEvent, 1.0f))
        } catch (exception: Exception) {
            SMetro.LOGGER.warn("Could not play station announcement {}", soundId, exception)
        }
    }
}
