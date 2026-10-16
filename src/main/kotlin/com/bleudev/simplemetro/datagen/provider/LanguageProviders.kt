package com.bleudev.simplemetro.datagen.provider

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider
import net.minecraft.core.HolderLookup
import java.util.concurrent.CompletableFuture

class SMetroDefaultLanguage(output: FabricPackOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) : FabricLanguageProvider(output, registryLookup) {
    override fun generateTranslations(registryLookup: HolderLookup.Provider, builder: TranslationBuilder) {
        builder.add("smetro.line.name.default", "New line")
        builder.add("smetro.line.name.default.id", "Line #%d")
        builder.add("commands.metro.station.create.success", "Station with id %d was created!")
        builder.add("commands.metro.station.create.error.position", "Invalid station position")
        builder.add("commands.metro.station.update.success", "Station was updated!")
        builder.add("commands.metro.station.update.error.status", "Unknown status")
        builder.add("commands.metro.station.update.error.dont_exist", "Station with id %d don't exist")
        builder.add("commands.metro.line.success.updated", "Line was updated successfully!")
        builder.add("commands.metro.line.success.new", "New line was created!")
        builder.add("commands.metro.line.error.color", "Invalid color format")
        builder.add("commands.metro.stations.error.unknown", "Unknown station with id: %d")
        builder.add("xaero.map.station.tooltip.status.closed_temp", "(closed temporary)")
        builder.add("xaero.map.station.tooltip.status.closed_repair", "(closed for repair)")
        builder.add("xaero.map.station.tooltip.status.under_construction", "(under construction)")
        builder.add("xaero.map.station.tooltip.status.planned", "(planned)")
        builder.add("xaero.map.station.tooltip.status.unknown", "(unknown status)")
        builder.add("xaero.map.station.tooltip.transfers.one", "Transfer to the station")
        builder.add("xaero.map.station.tooltip.transfers.some", "Transfers at stations")
        builder.add("xaero.map.station.tooltip.id", "ID: %d")
    }
}

class SMetroRussianLanguage(output: FabricPackOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) : FabricLanguageProvider(output, "ru_ru", registryLookup) {
    override fun generateTranslations(registryLookup: HolderLookup.Provider, builder: TranslationBuilder) {
        builder.add("smetro.line.name.default", "Новая ветка")
        builder.add("smetro.line.name.default.id", "Ветка №%d")
        builder.add("commands.metro.station.create.success", "Станция с айди %d была создана!")
        builder.add("commands.metro.station.create.error.position", "Неверная позиция станции")
        builder.add("commands.metro.station.update.success", "Станция была успешно обновлена!")
        builder.add("commands.metro.station.update.error.status", "Неизвестный статус")
        builder.add("commands.metro.station.update.error.dont_exist", "Станция с айди %d не существует")
        builder.add("commands.metro.line.success.updated", "Ветка была успешно обновлена!")
        builder.add("commands.metro.line.success.new", "Новая ветка была создана!")
        builder.add("commands.metro.line.error.color", "Неверный формат цвета")
        builder.add("commands.metro.stations.error.unknown", "Станция с айди %d не найдена")
        builder.add("xaero.map.station.tooltip.status.closed_temp", "(временно закрыто)")
        builder.add("xaero.map.station.tooltip.status.closed_repair", "(закрыто на ремонт)")
        builder.add("xaero.map.station.tooltip.status.under_construction", "(в процессе строительства)")
        builder.add("xaero.map.station.tooltip.status.planned", "(запланировано)")
        builder.add("xaero.map.station.tooltip.status.unknown", "(неизвестный статус)")
        builder.add("xaero.map.station.tooltip.transfers.one", "Пересадка на станцию")
        builder.add("xaero.map.station.tooltip.transfers.some", "Пересадка на станции")
        builder.add("xaero.map.station.tooltip.id", "Айди: %d")
    }
}
