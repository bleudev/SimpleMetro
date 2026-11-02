package com.bleudev.simplemetro

import com.bleudev.simplemetro.announcement.ServerAnnouncementTracker
import com.bleudev.simplemetro.custom.SMetroPacketPayloads
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SMetro : ModInitializer {
    override fun onInitialize() {
        SMetroPacketPayloads.initialize()
        ServerAnnouncementTracker.initialize()
    }

    companion object {
        const val ID = "simplemetro"

        fun resolve(path: String): Identifier {
            return Identifier.fromNamespaceAndPath(ID, path)
        }

        val LOGGER: Logger = LoggerFactory.getLogger("SimpleMetro")
    }
}
