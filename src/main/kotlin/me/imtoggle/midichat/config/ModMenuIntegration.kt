package me.imtoggle.midichat.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.LabelOption
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.StateManager
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import net.minecraft.network.chat.Component

class ModMenuIntegration : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parentScreen ->
            return@ConfigScreenFactory YetAnotherConfigLib.create(ModConfig.CONFIG) { default, current, builder -> builder
                .title(Component.literal("MidiChat"))
                .category(ConfigCategory.createBuilder()
                    .name(Component.literal("General"))
                    .option(
                        LabelOption.create(Component.literal("\"/midi rescan\" to rescan midi devices"))
                    )
                    .option(
                        LabelOption.create(Component.literal("\"/midi instrument <>\" to switch instrument"))
                    )
                    .option(Option.createBuilder<Int>()
                        .name(Component.literal("Send Volumes"))
                        .stateManager(StateManager.createSimple(
                            default.sendVolume, { current.sendVolume }, { value -> current.sendVolume = value })
                        )
                        .controller { option -> IntegerSliderControllerBuilder.create(option)
                            .range(0, 500)
                            .step(1)
                            .formatValue { value -> Component.literal("%d%%".format(value)) }
                        }
                        .build()
                    )
                    .option(Option.createBuilder<Int>()
                        .name(Component.literal("Local Volumes"))
                        .stateManager(StateManager.createSimple(
                            default.localVolume, { current.localVolume }, { value -> current.localVolume = value })
                        )
                        .controller { option -> IntegerSliderControllerBuilder.create(option)
                            .range(0, 500)
                            .step(1)
                            .formatValue { value -> Component.literal("%d%%".format(value)) }
                        }
                        .build()
                    )
                    .build()
                )
            }.generateScreen(parentScreen)
        }
    }
}