package com.bleudev.simplemetro.config.obj

import kotlinx.serialization.Serializable
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Serializable
data class Announcements(
    var arrival: String? = null,
    var departure: String? = null,
    @Suppress("ReplaceWithEnumMap")
    val statuses: HashMap<MetroStation.Status, String?> = hashMapOf()
) {
    constructor(
        arrival: Optional<String>,
        departure: Optional<String>,
        statuses: Map<MetroStation.Status, Optional<String>>,
    ) : this(
        arrival.getOrNull(),
        departure.getOrNull(),
        HashMap(statuses.mapValues { (_, v) -> v.getOrNull() }),
    )

    fun get(type: AnnouncementType): String? = when (type) {
        AnnouncementType.ARRIVAL -> arrival
        AnnouncementType.DEPARTURE -> departure
    }


    enum class AnnouncementType {
        ARRIVAL, DEPARTURE
    }

    companion object {
        private val OPTIONAL_STRING_CODEC = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional)
        private fun <T : Any> optionalGetter(getter: (Announcements) -> T?): (Announcements) -> Optional<T> {
            return { Optional.ofNullable(getter(it)) }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Announcements> = StreamCodec.composite(
            OPTIONAL_STRING_CODEC, optionalGetter(Announcements::arrival),
            OPTIONAL_STRING_CODEC, optionalGetter(Announcements::departure),
            ByteBufCodecs.map(::HashMap, MetroStation.Status.STREAM_CODEC, OPTIONAL_STRING_CODEC), { it.statuses.toMap().mapValues { (_, v) -> Optional.ofNullable(v) } },
            ::Announcements
        )

        val SETTERS: Map<String, (Announcements, String?) -> Unit> = mapOf(
            "arrival" to Announcements::arrival::set,
            "departure" to Announcements::departure::set,
        ).plus(MetroStation.Status.entries.toList().map { it.name.lowercase() to { a, v -> a.statuses[it] = v } })
    }
}