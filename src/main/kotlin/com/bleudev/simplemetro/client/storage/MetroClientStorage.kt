package com.bleudev.simplemetro.client.storage

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer

@Environment(EnvType.CLIENT)
class MetroClientStorage private constructor() {
    var currentStation: Int? = null
    var prevStation: Int? = null
    var nextStation: Int? = null
    var currentLine: Int? = null
    var stationUpdated: Boolean = false
    var stationProgress: Float? = null

    fun onPlayerTick(player: LocalPlayer, level: ClientLevel) {
        LineTableExtractor.getInstance().playerTick(this, player, level)
        println("DEBUG $currentLine $prevStation $currentStation $nextStation")
    }

    companion object {
        private var instance: MetroClientStorage? = null

        fun getInstance(): MetroClientStorage {
            if (instance == null) {
                instance = MetroClientStorage()
            }
            return instance!!
        }
    }
}