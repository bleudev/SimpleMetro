package com.bleudev.simplemetro.datagen.provider

import com.bleudev.simplemetro.client.config.SMetroConfig.SerializableConfig.Panel.PanelPosition
import com.bleudev.simplemetro.util.ConfigTranslationKeyBuilder
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider
import net.minecraft.core.HolderLookup
import java.util.concurrent.CompletableFuture

private class WrappedTranslate(private val builder: FabricLanguageProvider.TranslationBuilder) {
    var tb = ConfigTranslationKeyBuilder.default()

    fun translate(s: String, mod: ConfigTranslationKeyBuilder.() -> ConfigTranslationKeyBuilder = {this}) {
        builder.add(tb.mod().toString(), s)
    }
}

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
        builder.add("commands.metro.generic.announcement.id.error.unknown", "Unknown announcement file!")

        builder.add("xaero.map.station.tooltip.status.closed_temp", "(closed temporary)")
        builder.add("xaero.map.station.tooltip.status.closed_repair", "(closed for repair)")
        builder.add("xaero.map.station.tooltip.status.under_construction", "(under construction)")
        builder.add("xaero.map.station.tooltip.status.planned", "(planned)")
        builder.add("xaero.map.station.tooltip.status.unknown", "(unknown status)")
        builder.add("xaero.map.station.tooltip.transfers.one", "Transfer to the station")
        builder.add("xaero.map.station.tooltip.transfers.some", "Transfers at stations")
        builder.add("xaero.map.station.tooltip.id", "ID: %d")

        builder.add("smetro.hud.station", "Station")
        builder.add("smetro.hud.here", "Here")
        builder.add("smetro.hud.next_short", "Next")
        builder.add("smetro.hud.next", "The next station is")
        builder.add("smetro.hud.terminal", "—")

        builder.add("smetro.voicechat.category.announcements", "Announcements")
        builder.add("smetro.voicechat.category.announcements.description", "Metro Announcements")

        WrappedTranslate(builder).run {
            tb = tb.category("root")
            translate("Simple Metro Config")
            translate("Config of Simple Metro mod") {description()}

            tb = tb.root()

            // Scheme
            tb = tb.label("comment_scheme")
            translate("Scheme")
            tb = tb.option("show_on_xaero_map")
            translate("Show on Xaero's Map")
            translate("Show panel on Xaero's World Map") {description()}
            tb = tb.option("show_on_xaero_minimap")
            translate("Show on Xaero's Minimap")
            translate("Show panel on Xaero's Minimap") {description()}

            // Panel
            tb = tb.label("comment_panel")
            translate("Panel")
            tb = tb.option("show_panel")
            translate("Show")
            translate("Show panel during transportation") {description()}
            tb = tb.option("panel_position")
            translate("Position")
            translate("Panel position") {description()}

            translate("Top left") {enum(PanelPosition.TOP_LEFT)}
            translate("Top") {enum(PanelPosition.TOP)}
            translate("Top right") {enum(PanelPosition.TOP_RIGHT)}
            translate("Bottom left") {enum(PanelPosition.BOTTOM_LEFT)}
            translate("Bottom") {enum(PanelPosition.BOTTOM)}
            translate("Bottom right") {enum(PanelPosition.BOTTOM_RIGHT)}
        }
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
        builder.add("commands.metro.generic.announcement.id.error.unknown", "Неизвестный файл оповещения!")

        builder.add("xaero.map.station.tooltip.status.closed_temp", "(временно закрыто)")
        builder.add("xaero.map.station.tooltip.status.closed_repair", "(закрыто на ремонт)")
        builder.add("xaero.map.station.tooltip.status.under_construction", "(в процессе строительства)")
        builder.add("xaero.map.station.tooltip.status.planned", "(запланировано)")
        builder.add("xaero.map.station.tooltip.status.unknown", "(неизвестный статус)")
        builder.add("xaero.map.station.tooltip.transfers.one", "Пересадка на станцию")
        builder.add("xaero.map.station.tooltip.transfers.some", "Пересадка на станции")
        builder.add("xaero.map.station.tooltip.id", "Айди: %d")

        builder.add("smetro.hud.station", "Станция")
        builder.add("smetro.hud.here", "Текущая")
        builder.add("smetro.hud.next_short", "Следующая")
        builder.add("smetro.hud.next", "Следующая станция")
        builder.add("smetro.hud.terminal", "—")

        builder.add("smetro.voicechat.category.announcements", "Оповещения")
        builder.add("smetro.voicechat.category.announcements.description", "Оповещения в метро")

        WrappedTranslate(builder).run {
            tb = tb.category("root")
            translate("Конфиг Simple Metro")
            translate("Конфиг мода Simple Metro") {description()}

            tb = tb.root()
            // Scheme
            tb = tb.label("comment_scheme")
            translate("Схема")
            tb = tb.option("show_on_xaero_map")
            translate("Показать на карте Xaero")
            translate("Показать схему на Xaero's World Map") {description()}
            tb = tb.option("show_on_xaero_minimap")
            translate("Показать на миникарте Xaero")
            translate("Показать схему на Xaero's Minimap") {description()}

            // Panel
            tb = tb.label("comment_panel")
            translate("Панель")
            tb = tb.option("show_panel")
            translate("Показать")
            translate("Показать панель во время движения") {description()}
            tb = tb.option("panel_position")
            translate("Позиция")
            translate("Позиция панели") {description()}

            translate("Вверху слева") {enum(PanelPosition.TOP_LEFT)}
            translate("Вверху") {enum(PanelPosition.TOP)}
            translate("Вверху справа") {enum(PanelPosition.TOP_RIGHT)}
            translate("Снизу слева") {enum(PanelPosition.BOTTOM_LEFT)}
            translate("Снизу") {enum(PanelPosition.BOTTOM)}
            translate("Снизу справа") {enum(PanelPosition.BOTTOM_RIGHT)}
        }
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
        builder.add("commands.metro.generic.announcement.id.error.unknown", "Неизвѣстный файлъ оповѣщенія!")

        builder.add("xaero.map.station.tooltip.status.closed_temp", "(временно закрыта)")
        builder.add("xaero.map.station.tooltip.status.closed_repair", "(закрыта на ремонтъ)")
        builder.add("xaero.map.station.tooltip.status.under_construction", "(строится)")
        builder.add("xaero.map.station.tooltip.status.planned", "(запланирована)")
        builder.add("xaero.map.station.tooltip.status.unknown", "(неизвѣстный статусъ)")
        builder.add("xaero.map.station.tooltip.transfers.one", "Пересадка на станцію")
        builder.add("xaero.map.station.tooltip.transfers.some", "Пересадки на станціи")
        builder.add("xaero.map.station.tooltip.id", "Номеръ: %d")

        builder.add("smetro.hud.station", "Станція")
        builder.add("smetro.hud.here", "Текущая")
        builder.add("smetro.hud.next_short", "Слѣдующая")
        builder.add("smetro.hud.next", "Слѣдующая станція")
        builder.add("smetro.hud.terminal", "—")

        builder.add("smetro.voicechat.category.announcements", "Оповѣщенія")
        builder.add("smetro.voicechat.category.announcements.description", "Оповѣщенія въ метро")

        WrappedTranslate(builder).run {
            tb = tb.category("root")
            translate("Настройки Simple Metro")
            translate("Настройки дополненія Simple Metro") { description() }

            tb = tb.root()
            // Scheme
            tb = tb.label("comment_scheme")
            translate("Схема")
            tb = tb.option("show_on_xaero_map")
            translate("На картѣ Xaero's World Map")
            translate("Отображать схему на картѣ мира Xaero's World Map") { description() }
            tb = tb.option("show_on_xaero_minimap")
            translate("На мини-картѣ Xaero's Minimap")
            translate("Отображать схему на мини-картѣ Xaero's Minimap") { description() }

            // Panel
            tb = tb.label("comment_panel")
            translate("Панель")
            tb = tb.option("show_panel")
            translate("Отображать")
            translate("Отображать панель во время движенія") { description() }
            tb = tb.option("panel_position")
            translate("Положеніе")
            translate("Положеніе панели на экранѣ") { description() }

            translate("Сверху слѣва") { enum(PanelPosition.TOP_LEFT) }
            translate("Сверху") { enum(PanelPosition.TOP) }
            translate("Сверху справа") { enum(PanelPosition.TOP_RIGHT) }
            translate("Снизу слѣва") { enum(PanelPosition.BOTTOM_LEFT) }
            translate("Снизу") { enum(PanelPosition.BOTTOM) }
            translate("Снизу справа") { enum(PanelPosition.BOTTOM_RIGHT) }
        }
    }
}
