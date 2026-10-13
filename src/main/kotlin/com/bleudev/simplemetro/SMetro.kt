package com.bleudev.simplemetro

import com.bleudev.simplemetro.custom.SMetroPacketPayloads
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier

class SMetro : ModInitializer {
    override fun onInitialize() {
        SMetroPacketPayloads.initialize()
    }

    companion object {
        const val MOD_ID = "simplemetro"

        fun resolve(path: String): Identifier {
            return Identifier.fromNamespaceAndPath(MOD_ID, path)
        }
    }
}
