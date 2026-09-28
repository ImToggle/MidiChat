package me.imtoggle.midichat.core

import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent

class MidiPlugin : VoicechatPlugin {

    override fun getPluginId(): String = "midichat"

    override fun registerEvents(registration: EventRegistration) {
        registration.registerEvent(ClientVoicechatConnectionEvent::class.java) { event ->
            SoundManager.onConnection(event)
        }
        registration.registerEvent(MergeClientSoundEvent::class.java) { event ->
            SoundManager.onMergeSound(event)
        }
    }
}