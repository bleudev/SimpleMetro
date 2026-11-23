package com.bleudev.simplemetro.client.config

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.client.config.SMetroConfig.SerializableConfig
import com.bleudev.simplemetro.client.render.LinePanelRenderer
import com.bleudev.simplemetro.compat.ModCompat
import com.bleudev.simplemetro.compat.ModMenuCompat
import com.bleudev.simplemetro.util.ConfigTranslationKeyBuilder
import com.bleudev.simplemetro.util.file.AdvancedJsonReader
import dev.isxander.yacl3.api.NameableEnum
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.dsl.OptionDsl
import dev.isxander.yacl3.dsl.YetAnotherConfigLib
import dev.isxander.yacl3.dsl.available
import dev.isxander.yacl3.dsl.descriptionBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

@Environment(EnvType.CLIENT)
object SMetroConfig : AdvancedJsonReader<SerializableConfig>(
    FabricLoader.getInstance().configDir.resolve(SMetro.ID).resolve("client.config.json"),
    "Failed to load client config.",
    "Failed to save client config.",
    SerializableConfig.serializer(),
    ::SerializableConfig,
), ModMenuCompat {
    @Serializable
    data class SerializableConfig(
        var scheme: Scheme = Scheme(),
        var panel: Panel = Panel(),
    ) {
        @Serializable
        data class Scheme(
            @SerialName("show_on_xaero_map")
            var showOnXaeroMap: Boolean = false,
            @SerialName("show_on_xaero_minimap")
            var showOnXaeroMinimap: Boolean = false
        )

        @Serializable
        data class Panel(
            var show: Boolean = true,
            var position: PanelPosition = PanelPosition.TOP_RIGHT,
            @SerialName("show_remaining_distance")
            var showRemainingDistance: Boolean = true,
            @SerialName("show_remaining_time")
            var showRemainingTime: Boolean = true,
        ) {
            enum class PanelPosition : NameableEnum {
                TOP_LEFT {
                    override fun position(guiWidth: Int, guiHeight: Int, slide: Float): Pair<Int, Int> {
                        var dx = 0
                        if (ModCompat.CLIENT_SVC.getOrNull()?.isInGroup() == true) {
                            dx += 20
                        }
                        return start(P_WIDTH, slide) + dx to start(P_HEIGHT, 1f)
                    }
                }, TOP {
                    override fun position(guiWidth: Int, guiHeight: Int, slide: Float): Pair<Int, Int> {
                        return center(P_WIDTH, guiWidth) to start(P_HEIGHT, slide)
                    }
                }, TOP_RIGHT {
                    override fun position(guiWidth: Int, guiHeight: Int, slide: Float): Pair<Int, Int> {
                        return end(P_WIDTH, guiWidth, slide) to start(P_HEIGHT, 1f)
                    }
                },
                BOTTOM_LEFT {
                    override fun position(guiWidth: Int, guiHeight: Int, slide: Float): Pair<Int, Int> {
                        return start(P_WIDTH, slide) to end(P_HEIGHT, guiHeight, 1f) - margin()
                    }
                }, BOTTOM {
                    override fun position(guiWidth: Int, guiHeight: Int, slide: Float): Pair<Int, Int> {
                        return center(P_WIDTH, guiWidth) to end(P_HEIGHT, guiHeight, slide) - margin()
                    }
                }, BOTTOM_RIGHT {
                    override fun position(guiWidth: Int, guiHeight: Int, slide: Float): Pair<Int, Int> {
                        return end(P_WIDTH, guiWidth, slide) to end(P_HEIGHT, guiHeight, 1f) - margin()
                    }
                };

                override fun getDisplayName(): Component {
                    return Component.translatable(ConfigTranslationKeyBuilder.default().enum(this).build())
                }

                abstract fun position(guiWidth: Int, guiHeight: Int, slide: Float): Pair<Int, Int>

                companion object {
                    private const val P_MARGIN = LinePanelRenderer.PANEL_MARGIN
                    private const val P_WIDTH = LinePanelRenderer.PANEL_WIDTH
                    private const val P_HEIGHT = LinePanelRenderer.PANEL_HEIGHT

                    private fun margin(): Int {
                        var result = 25
                        val player = Minecraft.getInstance().player ?: return result
                        if (player.gameMode()?.isSurvival == true) {
                            result += 10
                            if (player.armorValue > 0) {
                                result += 10
                            }
                        }
                        return result
                    }

                    private fun start(side: Int, slide: Float): Int {
                        return P_MARGIN + ((slide - 1f) * side).roundToInt()
                    }
                    private fun center(side: Int, entire: Int): Int {
                        return (entire - side) / 2
                    }
                    private fun end(side: Int, entire: Int, slide: Float): Int {
                        return entire - P_MARGIN - (slide * side).roundToInt()
                    }
                }
            }

            fun shouldRender(): Boolean {
                if (!show) return false
                if (position == PanelPosition.BOTTOM_LEFT) {
                    return Minecraft.getInstance().gui.screen() !is ChatScreen
                }
                return true
            }
        }
    }

    override fun <P : Screen> gui(parent: P?): Screen {
        val config = load()
        return YetAnotherConfigLib(SMetro.ID) {
            save { save(config) }
            categories.register("root") {
                groups.register("scheme") {
                    options.register<Boolean>("show_on_xaero_map") {
                        controller { BooleanControllerBuilder.create(it).yesNoFormatter().coloured(true) }
                        binding(true, config.scheme::showOnXaeroMap, config.scheme::showOnXaeroMap::set)
                        descriptionBuilder {
                            addDefaultText(1)
                        }
                        available { FabricLoader.getInstance().isModLoaded("xaeroworldmap") }
                    }
                    options.register<Boolean>("show_on_xaero_minimap") {
                        controller { BooleanControllerBuilder.create(it).yesNoFormatter().coloured(true) }
                        binding(true, config.scheme::showOnXaeroMinimap, config.scheme::showOnXaeroMinimap::set)
                        descriptionBuilder {
                            addDefaultText(1)
                        }
                        available { FabricLoader.getInstance().isModLoaded("xaerominimap") }
                    }
                }

                groups.register("panel") {
                    val dependsOnShowPanelIds = arrayListOf<String>()
                    fun <T : Any> OptionDsl<T>.dependsOnShowPanel(availableCondition: () -> Boolean = {true}) {
                        dependsOnShowPanelIds.add(optionId)
                        available { availableCondition() && options.futureRef<Boolean>("show_panel").getNow(null)?.pendingValue() ?: true }
                    }

                    options.register<Boolean>("show_panel") {
                        controller { BooleanControllerBuilder.create(it).yesNoFormatter().coloured(true) }
                        addListener { option, _ ->
                            val v = option.pendingValue()

                            for (id in dependsOnShowPanelIds) {
                                options.futureRef<Any>(id).getNow(null)?.setAvailable(v)
                            }

                        }
                        binding(true, config.panel::show, config.panel::show::set)
                        descriptionBuilder {
                            addDefaultText(1)
                        }
                    }
                    options.register<SerializableConfig.Panel.PanelPosition>("panel_position") {
                        controller {
                            EnumControllerBuilder.create<SerializableConfig.Panel.PanelPosition>(it)
                                ?.enumClass(SerializableConfig.Panel.PanelPosition::class.java)
                        }
                        binding(
                            SerializableConfig.Panel.PanelPosition.TOP_RIGHT,
                            config.panel::position,
                            config.panel::position::set
                        )
                        descriptionBuilder {
                            addDefaultText(1)
                        }
                        dependsOnShowPanel()
                    }
                    options.register<Boolean>("show_remaining_distance") {
                        controller { BooleanControllerBuilder.create(it).yesNoFormatter().coloured(true) }
                        binding(true, config.panel::showRemainingDistance, config.panel::showRemainingDistance::set)
                        descriptionBuilder {
                            addDefaultText(1)
                        }
                    }
                    options.register<Boolean>("show_remaining_time") {
                        controller { BooleanControllerBuilder.create(it).yesNoFormatter().coloured(true) }
                        binding(true, config.panel::showRemainingTime, config.panel::showRemainingTime::set)
                        descriptionBuilder {
                            addDefaultText(1)
                        }
                    }
                }
            }
        }.generateScreen(parent)
    }

    override fun load(): SerializableConfig {
        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            return super.load()
        } else {
            throw IllegalStateException("Simple Metro client config can be loaded only on client side!")
        }
    }

    override fun save(value: SerializableConfig) {
        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            super.save(value)
        } else {
            throw IllegalStateException("Simple Metro client config can be saved only on client side!")
        }
    }
}