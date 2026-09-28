package me.imtoggle.midichat.core

import de.maxhenkel.voicechat.api.*
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent
import net.minecraft.client.sounds.JOrbisAudioStream
import net.minecraft.resources.ResourceLocation
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.pow

object SoundManager {

    private var clientApi: VoicechatClientApi? = null
    private var localAudioChannel: ClientStaticAudioChannel? = null
    private val playingSounds = ConcurrentLinkedQueue<NoteInfo>()
    var currentSound = ResourceLocation.withDefaultNamespace("sounds/note/harp.ogg")

    fun onConnection(event: ClientVoicechatConnectionEvent) {
        if (event.isConnected) {
            clientApi = event.voicechat
            val category = clientApi!!.volumeCategoryBuilder()
                .setId("midichat")
                .setName("MidiChat")
                .build()
            clientApi!!.registerClientVolumeCategory(category)
            localAudioChannel = clientApi!!.createStaticAudioChannel(UUID.randomUUID())
            localAudioChannel?.category = category.id
        } else {
            clientApi = null
            localAudioChannel = null
            playingSounds.clear()
        }
    }

    fun onMergeSound(event: MergeClientSoundEvent) {
        if (event.voicechat.isDisabled) {
            playingSounds.clear()
            return
        }
        if (playingSounds.isEmpty()) return

        val sendAudio = ShortArray(960)
        val localAudio = ShortArray(960)
        var hasAudio = false

        val iterator = playingSounds.iterator()
        while (iterator.hasNext()) {
            val sound = iterator.next()

            if (sound.isFinished) {
                iterator.remove()
                continue
            }

            hasAudio = true

            for (i in 0 until min(960, sound.remaining)) {
                val rawSample = sound.readNext()
                mixSample(sendAudio, i, rawSample, sound.volume * config.sendVolume / 100f)
                mixSample(localAudio, i, rawSample, sound.volume * config.localVolume / 100f)
            }
        }

        if (hasAudio) {
            event.mergeAudio(sendAudio)
            localAudioChannel?.play(localAudio)
        }
    }

    private fun mixSample(buffer: ShortArray, index: Int, sample: Short, volume: Float) {
        val weightedSample = (sample * volume).toInt()
        var result = buffer[index] + weightedSample

        if (result > Short.MAX_VALUE) result = Short.MAX_VALUE.toInt()
        else if (result < Short.MIN_VALUE) result = Short.MIN_VALUE.toInt()

        buffer[index] = result.toShort()
    }

    fun playSound(key: Int, velocity: Int) {
        if (mc.resourceManager == null || clientApi == null) return
        try {
            val targetPitch = 2.0.pow((key - 66) / 12.0).toFloat()
            playingSounds.add(loadSound(volume = velocity / 127f, pitch = targetPitch))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSound(volume: Float = 1f, pitch: Float = 1f): NoteInfo {
        val resource = mc.resourceManager.getResource(currentSound)
            .orElseThrow { IllegalArgumentException("Couldn't find sound: $currentSound") }

        val (rawSamples, channels, sourceSampleRate) = resource.open().use { inputStream ->
            JOrbisAudioStream(inputStream).use { stream ->
                val format = stream.format
                val buffer = stream.readAll().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                val samples = ShortArray(buffer.remaining())
                buffer.get(samples)
                Triple(samples, format.channels, format.sampleRate.toInt())
            }
        }

        val monoSamples = if (channels == 1) rawSamples else downmixToMono(rawSamples, channels)

        val finalSamples = if (sourceSampleRate == 48000 && pitch == 1.0f) {
            monoSamples
        } else {
            resampleLinear(monoSamples, sourceSampleRate, pitch)
        }

        return NoteInfo(finalSamples, volume)
    }

    private fun downmixToMono(rawSamples: ShortArray, channels: Int) = ShortArray(rawSamples.size / channels) { i ->
        var sum = 0
        for (c in 0 until channels) {
            sum += rawSamples[i * channels + c]
        }
        (sum / channels).toShort()
    }

    private fun resampleLinear(input: ShortArray, sourceRate: Int, pitch: Float): ShortArray {
        val ratio = sourceRate.toDouble() * pitch / 48000.0

        return ShortArray((input.size / ratio).toInt()) { i ->
            val sourcePos = i * ratio
            val index = sourcePos.toInt()

            if (index >= input.size - 1) {
                input.last()
            } else {
                val fraction = (sourcePos - index).toFloat()
                val current = input[index].toFloat()
                val next = input[index + 1].toFloat()
                (current + fraction * (next - current)).toInt().toShort()
            }
        }
    }

    class NoteInfo(private val samples: ShortArray, val volume: Float) {

        private var cursor = 0

        val isFinished: Boolean
            get() = cursor >= samples.size

        val remaining: Int
            get() = samples.size - cursor

        fun readNext(): Short {
            return if (cursor < samples.size) samples[cursor++] else 0
        }
    }

}