package com.bleudev.simplemetro.config.obj

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object StationStatusSerializer : KSerializer<MetroStation.Status> {
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("MetroStation.Status", String.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: MetroStation.Status) {
        String.serializer().serialize(encoder, value.serializedName)
    }

    override fun deserialize(decoder: Decoder): MetroStation.Status {
        val name = String.serializer().deserialize(decoder).lowercase()
        return MetroStation.Status.fromNameOrFallback(name)
    }
}