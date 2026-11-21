package com.bleudev.simplemetro.compat

import net.minecraft.client.gui.screens.Screen

@FunctionalInterface
interface ModMenuCompat {
    fun <P : Screen> gui(parent: P): Screen
}