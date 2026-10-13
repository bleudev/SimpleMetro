package com.bleudev.simplemetro.obj

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.core.BlockPos

object BlockPosSerializer : KSerializer<BlockPos> {
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("BlockPos", IntArraySerializer().descriptor)

    override fun serialize(encoder: Encoder, value: BlockPos) {
        ListSerializer(Int.serializer()).serialize(encoder, listOf(value.x, value.y, value.z))
    }

    override fun deserialize(decoder: Decoder): BlockPos {
        val coords = ListSerializer(Int.serializer()).deserialize(decoder)
        assert(coords.size == 3) { "BlockPos can have only 3 dimensions exact." }
        return BlockPos(coords[0], coords[1], coords[2])
    }
}