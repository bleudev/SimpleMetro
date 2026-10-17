package com.bleudev.simplemetro.datagen

import com.bleudev.simplemetro.datagen.provider.SMetroDefaultLanguage
import com.bleudev.simplemetro.datagen.provider.SMetroPreReformRussianLanguage
import com.bleudev.simplemetro.datagen.provider.SMetroRussianLanguage
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class SMetroDataGen : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()
        // Languages
        pack.addProvider(::SMetroDefaultLanguage)
        pack.addProvider(::SMetroRussianLanguage)
        pack.addProvider(::SMetroPreReformRussianLanguage)
    }
}
