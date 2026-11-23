package com.bleudev.simplemetro.util

import io.netty.buffer.ByteBuf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

interface SuggestionContext {
    val announcementIds: List<String>

    companion object {
        private val EMPTY = SuggestionContextImpl()
        internal fun create(): SuggestionContext = SuggestionContextImpl.create()

        @Environment(EnvType.CLIENT)
        private var syncedContext: SuggestionContext? = null
        @Environment(EnvType.CLIENT)
        internal fun sync(context: SuggestionContext) {
            syncedContext = context
        }
        @Environment(EnvType.CLIENT)
        fun get(): SuggestionContext {
            if (isModOnServer) {
                return syncedContext ?: EMPTY // Not synced yet
            }
            else {
                syncedContext = null
                return SuggestionContextImpl.create()
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SuggestionContext> = StreamCodec.composite(
            ByteBufCodecs.list<ByteBuf, String>().apply(ByteBufCodecs.STRING_UTF8), SuggestionContext::announcementIds,
            ::SuggestionContextImpl
        )
    }
}