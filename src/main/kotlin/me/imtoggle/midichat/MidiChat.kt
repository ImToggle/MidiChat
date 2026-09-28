package me.imtoggle.midichat

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import me.imtoggle.midichat.config.ModConfig
import me.imtoggle.midichat.core.MidiInputHandler
import me.imtoggle.midichat.core.SoundManager
import me.imtoggle.midichat.core.config
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import javax.sound.midi.MidiSystem

class MidiChat : ClientModInitializer {

    override fun onInitializeClient() {
        ModConfig.CONFIG.load()
        setInstrument(config.instrument)
        rescanMidiDevice()
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClientCommandManager.literal("midi")
                .then(ClientCommandManager.literal("rescan").executes {
                    rescanMidiDevice()
                    1
                })
                .then(ClientCommandManager.literal("instrument")
                    .then(ClientCommandManager.argument("id", StringArgumentType.word())
                        .suggests(SuggestionProvider { _, builder ->
                            NoteBlockInstrument.entries.filter { it.isTunable }.forEach {
                                builder.suggest(it.serializedName)
                            }
                            return@SuggestionProvider builder.buildFuture()
                        })
                        .executes { context ->
                            setInstrument(StringArgumentType.getString(context, "id"))
                            1
                        }
                    )
                )
            )
        }
    }

    fun setInstrument(instrument: String) {
        NoteBlockInstrument.entries.find { it.serializedName == instrument }?.let {
            SoundManager.currentSound = ResourceLocation.withDefaultNamespace("sounds/note/$instrument.ogg")
            config.instrument = instrument
            ModConfig.CONFIG.save()
        }
    }

    fun rescanMidiDevice() {
        for (info in MidiSystem.getMidiDeviceInfo()) {
            val device = MidiSystem.getMidiDevice(info)
            if (device.maxTransmitters != 0) {
                val transmitter = device.transmitter
                transmitter.receiver = MidiInputHandler()
                device.open()
                println("MIDI Device connected: ${info.name}")
            }
        }
    }

}