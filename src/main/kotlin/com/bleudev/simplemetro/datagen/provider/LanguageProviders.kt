package com.bleudev.simplemetro.datagen.provider

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider
import net.minecraft.core.HolderLookup
import java.util.concurrent.CompletableFuture

class SMetroDefaultLanguage(output: FabricPackOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) : FabricLanguageProvider(output, registryLookup) {
    override fun generateTranslations(registryLookup: HolderLookup.Provider, builder: TranslationBuilder) {
        builder.add("smetro.line.name.default", "New line")
        builder.add("commands.metro.station.create.success", "Station was created!")
        builder.add("commands.metro.station.create.error.position", "Invalid station position")
        builder.add("commands.metro.line.success.updated", "Line was updated successfully!")
        builder.add("commands.metro.line.success.new", "New line was created!")
        builder.add("commands.metro.line.error.color", "Invalid color format")
        builder.add("commands.metro.line.stations.error.unknown", "Unknown station with id: %d")
        builder.add("xaero.map.station.tooltip.id", "ID: %d")
    }
}

class SMetroRussianLanguage(output: FabricPackOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) : FabricLanguageProvider(output, "ru_ru", registryLookup) {
    override fun generateTranslations(registryLookup: HolderLookup.Provider, builder: TranslationBuilder) {
        builder.add("smetro.line.name.default", "Новая ветка")
        builder.add("commands.metro.station.create.success", "Станция была создана!")
        builder.add("commands.metro.station.create.error.position", "Неверная позиция станции")
        builder.add("commands.metro.line.success.updated", "Ветка была успешно обновлена!")
        builder.add("commands.metro.line.success.new", "Новая ветка была создана!")
        builder.add("commands.metro.line.error.color", "Неверный формат цвета")
        builder.add("commands.metro.line.stations.error.unknown", "Станция с айди %d не найдена")
        builder.add("xaero.map.station.tooltip.id", "Айди: %d")
    }
}
