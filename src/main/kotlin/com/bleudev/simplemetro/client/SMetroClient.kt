package com.bleudev.simplemetro.client

import com.bleudev.simplemetro.client.util.MetroSchemeRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

class SMetroClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register { MetroSchemeRenderer.getInstance().tick() }
    }
}
