package com.bleudev.simplemetro.datagen.provider

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider
import net.minecraft.core.HolderLookup
import java.util.concurrent.CompletableFuture

class SMetroDefaultLanguage(output: FabricPackOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) : FabricLanguageProvider(output, registryLookup) {
    override fun generateTranslations(registryLookup: HolderLookup.Provider, builder: TranslationBuilder) {
        builder.add("smetro.line.name.default", "New line")
        builder.add("smetro.line.name.default.id", "Line #%d")
        builder.add("commands.metro.station.add.success", "Station with id %d was added!")
        builder.add("commands.metro.station.add.error.position", "Invalid station position")
        builder.add("commands.metro.station.remove.success", "Station was deleted!")
        builder.add("commands.metro.station.update.success", "Station was updated!")
        builder.add("commands.metro.station.update.error.status", "Unknown status")
        builder.add("commands.metro.station.place.add.success", "The station was successfully placed on the line with id %d")
        builder.add("commands.metro.station.place.add.success.line", "The station was successfully placed on a new line with id %d")
        builder.add("commands.metro.station.place.add.corner.error.corner_id", "The station with id %d is not an corner!")
        builder.add("commands.metro.station.place.add.between.error.not_neighbors", "The specified stations are not adjacent!")
        builder.add("commands.metro.station.place.remove.success", "The station was successfully removed from the line with id %d")
        builder.add("commands.metro.station.place.remove.error.not_placed", "The station is not placed on the line with id %d!")
        builder.add("commands.metro.line.success.updated", "Line was updated successfully!")
        builder.add("commands.metro.line.success.new", "New line was created!")
        builder.add("commands.metro.line.error.color", "Invalid color format")

        builder.add("commands.metro.generic.station.unknown", "Unknown station with id: %d")
        builder.add("commands.metro.generic.error.unknown", "Unknown error with code: %d. Please report this.")
        builder.add("commands.metro.generic.line.unknown", "Unknown line with id: %d")

        builder.add("xaero.map.station.tooltip.status.closed_temp", "(closed temporary)")
        builder.add("xaero.map.station.tooltip.status.closed_repair", "(closed for repair)")
        builder.add("xaero.map.station.tooltip.status.under_construction", "(under construction)")
        builder.add("xaero.map.station.tooltip.status.planned", "(planned)")
        builder.add("xaero.map.station.tooltip.status.unknown", "(unknown status)")
        builder.add("xaero.map.station.tooltip.transfers.one", "Transfer to the station")
        builder.add("xaero.map.station.tooltip.transfers.some", "Transfers at stations")
        builder.add("xaero.map.station.tooltip.id", "ID: %d")

        builder.add("smetro.hud.route", "METRO ROUTE")
        builder.add("smetro.hud.current", "CURRENT STATION")
        builder.add("smetro.hud.from", "LAST STATION")
        builder.add("smetro.hud.terminal", "Terminal station")
        builder.add("smetro.hud.transfers", "TRANSFERS")
        builder.add("smetro.hud.no_transfers", "No transfers")
        builder.add("smetro.hud.event.arrival", "ARRIVED AT")
        builder.add("smetro.hud.event.departure", "DEPARTING FROM")
        builder.add("smetro.hud.no_line", "NO LINE")
        builder.add("smetro.hud.no_next_station", "—")
        builder.add("smetro.hud.here", "HERE")
        builder.add("smetro.hud.next_short", "NEXT")
        builder.add("smetro.hud.next", "The next station is")
        builder.add("smetro.hud.from_short", "FROM")
    }
}

class SMetroRussianLanguage(output: FabricPackOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) : FabricLanguageProvider(output, "ru_ru", registryLookup) {
    override fun generateTranslations(registryLookup: HolderLookup.Provider, builder: TranslationBuilder) {
        builder.add("smetro.line.name.default", "Новая линия")
        builder.add("smetro.line.name.default.id", "Линия №%d")
        builder.add("commands.metro.station.add.success", "Станция с айди %d была создана!")
        builder.add("commands.metro.station.add.error.position", "Неверная позиция станции")
        builder.add("commands.metro.station.remove.success", "Станция была удалена!")
        builder.add("commands.metro.station.update.success", "Станция была успешно обновлена!")
        builder.add("commands.metro.station.update.error.status", "Неизвестный статус")
        builder.add("commands.metro.station.place.add.success", "Станция была успешно размещена на линию с айди %d")
        builder.add("commands.metro.station.place.add.success.line", "Станция была успешно размещена на новую линию с айди %d")
        builder.add("commands.metro.station.place.add.corner.error.corner_id", "Станция с айди %d не является краем!")
        builder.add("commands.metro.station.place.add.between.error.not_neighbors", "Указанные станции не соседние!")
        builder.add("commands.metro.station.place.remove.success", "Станция была успешно удалена с линии с айди %d")
        builder.add("commands.metro.station.place.remove.error.not_placed", "Станция не размещена на линии с айди %d!")
        builder.add("commands.metro.line.success.updated", "Линия была успешно обновлена!")
        builder.add("commands.metro.line.success.new", "Новая линия была создана!")
        builder.add("commands.metro.line.error.color", "Неверный формат цвета")

        builder.add("commands.metro.generic.station.unknown", "Станция с айди %d не найдена")
        builder.add("commands.metro.generic.error.unknown", "Неизвестная ошибка с кодом: %d. Пожалуйста, сообщите об этом.")
        builder.add("commands.metro.generic.line.unknown", "Линия с айди %d не найдена")

        builder.add("xaero.map.station.tooltip.status.closed_temp", "(временно закрыто)")
        builder.add("xaero.map.station.tooltip.status.closed_repair", "(закрыто на ремонт)")
        builder.add("xaero.map.station.tooltip.status.under_construction", "(в процессе строительства)")
        builder.add("xaero.map.station.tooltip.status.planned", "(запланировано)")
        builder.add("xaero.map.station.tooltip.status.unknown", "(неизвестный статус)")
        builder.add("xaero.map.station.tooltip.transfers.one", "Пересадка на станцию")
        builder.add("xaero.map.station.tooltip.transfers.some", "Пересадка на станции")
        builder.add("xaero.map.station.tooltip.id", "Айди: %d")

        builder.add("smetro.hud.route", "МАРШРУТ МЕТРО")
        builder.add("smetro.hud.current", "ТЕКУЩАЯ СТАНЦИЯ")
        builder.add("smetro.hud.from", "ОТПРАВИЛИСЬ ОТ")
        builder.add("smetro.hud.terminal", "Конечная станция")
        builder.add("smetro.hud.transfers", "ПЕРЕСАДКИ")
        builder.add("smetro.hud.no_transfers", "Без пересадок")
        builder.add("smetro.hud.event.arrival", "ПРИБЫТИЕ НА СТАНЦИЮ")
        builder.add("smetro.hud.event.departure", "ОТПРАВЛЕНИЕ СО СТАНЦИИ")
        builder.add("smetro.hud.no_line", "БЕЗ ЛИНИИ")
        builder.add("smetro.hud.no_next_station", "—")
        builder.add("smetro.hud.here", "ЗДЕСЬ")
        builder.add("smetro.hud.next_short", "ДАЛЕЕ")
        builder.add("smetro.hud.next", "Следующая станция")
        builder.add("smetro.hud.from_short", "ОТ")
    }
}

class SMetroPreReformRussianLanguage(output: FabricPackOutput, registryLookup: CompletableFuture<HolderLookup.Provider>) : FabricLanguageProvider(output, "rpr", registryLookup) {
    override fun generateTranslations(registryLookup: HolderLookup.Provider, builder: TranslationBuilder) {
        builder.add("smetro.line.name.default", "Новая линія")
        builder.add("smetro.line.name.default.id", "Линія №%d")
        builder.add("commands.metro.station.add.success", "Станція №%d создана!")
        builder.add("commands.metro.station.add.error.position", "Невѣрное положеніе станціи")
        builder.add("commands.metro.station.remove.success", "Станція удалена!")
        builder.add("commands.metro.station.update.success", "Станція успѣшно обновлена!")
        builder.add("commands.metro.station.update.error.status", "Неизвѣстный статусъ")
        builder.add("commands.metro.station.place.add.success", "Станція успѣшно размещена на линіи №%d")
        builder.add("commands.metro.station.place.add.success.line", "Станція успѣшно размещена на новой линіи №%d")
        builder.add("commands.metro.station.place.add.corner.error.corner_id", "Станція №%d не является конечной!")
        builder.add("commands.metro.station.place.add.between.error.not_neighbors", "Указанныя станціи не сосѣднія!")
        builder.add("commands.metro.station.place.remove.success", "Станція успѣшно удалена съ линіи №%d")
        builder.add("commands.metro.station.place.remove.error.not_placed", "Станція не размещена на линіи №%d!")
        builder.add("commands.metro.line.success.updated", "Линія успѣшно обновлена!")
        builder.add("commands.metro.line.success.new", "Создана новая линія!")
        builder.add("commands.metro.line.error.color", "Невѣрный форматъ цвѣта")

        builder.add("commands.metro.generic.station.unknown", "Станція №%d не найдена")
        builder.add("commands.metro.generic.error.unknown", "Неизвѣстная ошибка съ кодомъ %d. Пожалуйста, сообщите объ этомъ.")
        builder.add("commands.metro.generic.line.unknown", "Линія №%d не найдена")

        builder.add("xaero.map.station.tooltip.status.closed_temp", "(временно закрыта)")
        builder.add("xaero.map.station.tooltip.status.closed_repair", "(закрыта на ремонтъ)")
        builder.add("xaero.map.station.tooltip.status.under_construction", "(строится)")
        builder.add("xaero.map.station.tooltip.status.planned", "(запланирована)")
        builder.add("xaero.map.station.tooltip.status.unknown", "(неизвѣстный статусъ)")
        builder.add("xaero.map.station.tooltip.transfers.one", "Пересадка на станцію")
        builder.add("xaero.map.station.tooltip.transfers.some", "Пересадки на станціи")
        builder.add("xaero.map.station.tooltip.id", "Номеръ: %d")

        builder.add("smetro.hud.route", "МАРШРУТЪ МЕТРО")
        builder.add("smetro.hud.current", "ТЕКУЩАЯ СТАНЦІЯ")
        builder.add("smetro.hud.from", "ОТПРАВЛЕНІЕ ОТЪ")
        builder.add("smetro.hud.terminal", "—")
        builder.add("smetro.hud.transfers", "ПЕРЕСАДКИ")
        builder.add("smetro.hud.no_transfers", "Безъ пересадокъ")
        builder.add("smetro.hud.event.arrival", "ПРИБЫТІЕ НА СТАНЦІЮ")
        builder.add("smetro.hud.event.departure", "ОТПРАВЛЕНІЕ СО СТАНЦІИ")
        builder.add("smetro.hud.no_line", "БЕЗЪ ЛИНІИ")
        builder.add("smetro.hud.no_next_station", "—")
        builder.add("smetro.hud.here", "ЗДѢСЬ")
        builder.add("smetro.hud.next_short", "ДАЛѢЕ")
        builder.add("smetro.hud.next", "Слѣдующая станція")
        builder.add("smetro.hud.from_short", "ОТЪ")
    }
}
