package com.bleudev.simplemetro.client.compat.modmenu

import com.bleudev.simplemetro.compat.ModCompat
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screens.Screen

@Environment(EnvType.CLIENT)
class SMetroModMenuAPI : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return SMetroScreenFactory()
    }

    private class SMetroScreenFactory : ConfigScreenFactory<Screen> {
        override fun create(parent: Screen?): Screen? {
            return ModCompat.MODMENU.getOrNull()?.gui(parent)
        }
    }
}