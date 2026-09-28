package me.imtoggle.midichat.core

import javax.sound.midi.MidiMessage
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage

class MidiInputHandler : Receiver {

    override fun send(message: MidiMessage, timeStamp: Long) {
        val shortMessage = message as? ShortMessage ?: return
        val command = shortMessage.command
        val key = shortMessage.getData1()
        val velocity = shortMessage.getData2()
        if (command == ShortMessage.NOTE_ON && velocity > 0) {
            onNoteOn(key, velocity)
        } else if (command == ShortMessage.NOTE_OFF || (command == ShortMessage.NOTE_ON && velocity == 0)) {
            onNoteOff(key)
        }
    }

    fun onNoteOn(key: Int, velocity: Int) {
        SoundManager.playSound(key, velocity)
    }

    fun onNoteOff(key: Int) {
    }

    override fun close() {
    }

}