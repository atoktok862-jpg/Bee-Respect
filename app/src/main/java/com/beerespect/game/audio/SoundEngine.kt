package com.beerespect.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.Collections
import kotlin.math.sin

enum class Sfx { STING, POLLEN, DASH, EXPLODE, HIT, WAVE, ULT, CLICK, BOSS }

class SoundEngine {
    private var track: AudioTrack? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    @Volatile private var enabled = true
    @Volatile private var engineActive = false
    private val effects = Collections.synchronizedList(ArrayList<Effect>())

    private data class Effect(val kind: Sfx, var age: Float, val duration: Float, var phase: Float)

    private val sampleRate = 22050
    private val sampleDt = 1f / 22050f
    private val melody = intArrayOf(
        69, 72, 76, 72, 69, 74, 77, 74,
        67, 71, 74, 71, 65, 69, 72, 69,
        69, 72, 76, 79, 77, 76, 72, 69,
        67, 71, 74, 76, 74, 71, 69, 67
    )
    private var melodyIndex = 0
    private var melodyTimer = 0f
    private var enginePhase = 0f
    private var noiseSeed = 12345

    fun start() {
        if (running) return
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 4)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = audioTrack
        running = true
        audioTrack.play()
        worker = Thread({ mix(audioTrack) }, "BeeAudioMixer").apply { isDaemon = true; start() }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun setEngineActive(value: Boolean) {
        engineActive = value
    }

    fun play(kind: Sfx) {
        if (!enabled) return
        val duration = when (kind) {
            Sfx.STING -> 0.12f
            Sfx.POLLEN -> 0.35f
            Sfx.DASH -> 0.42f
            Sfx.EXPLODE -> 0.5f
            Sfx.HIT -> 0.16f
            Sfx.WAVE -> 1.3f
            Sfx.ULT -> 1.5f
            Sfx.CLICK -> 0.08f
            Sfx.BOSS -> 1f
        }
        effects.add(Effect(kind, 0f, duration, 0f))
        while (effects.size > 16) effects.removeAt(0)
    }

    fun pause() {
        track?.pause()
    }

    fun resume() {
        if (running) track?.play()
    }

    fun stop() {
        running = false
        try { worker?.join(300) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        worker = null
        val current = track
        track = null
        current?.let {
            try {
                it.stop()
                it.release()
            } catch (_: IllegalStateException) {
            }
        }
        effects.clear()
    }

    private fun mix(audioTrack: AudioTrack) {
        val frames = 1024
        val pcm = ShortArray(frames)
        while (running) {
            if (!enabled) {
                java.util.Arrays.fill(pcm, 0.toShort())
                tryWrite(audioTrack, pcm)
                Thread.sleep(8)
                continue
            }
            var index = 0
            while (index < frames) {
                val sample = mixSample()
                val clamped = (sample * 24000f).toInt().coerceIn(-32768, 32767)
                pcm[index++] = clamped.toShort()
            }
            tryWrite(audioTrack, pcm)
        }
    }

    private fun tryWrite(audioTrack: AudioTrack, pcm: ShortArray) {
        try {
            if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.write(pcm, 0, pcm.size)
            }
        } catch (_: IllegalStateException) {
        }
    }

    private fun mixSample(): Float {
        var value = 0f
        val hum = if (engineActive) 0.035f else 0.014f
        enginePhase += (2.0 * kotlin.math.PI * 108.0 / sampleRate.toDouble()).toFloat()
        value += (sin(enginePhase) * 0.7f + sin(enginePhase * 2.01f) * 0.3f) * hum

        melodyTimer += sampleDt
        if (melodyTimer >= 0.26f) {
            melodyTimer -= 0.26f
            melodyIndex = (melodyIndex + 1) % melody.size
        }
        val midi = melody[melodyIndex]
        val freq = (440.0 * Math.pow(2.0, (midi - 69) / 12.0)).toFloat()
        val notePhase = ((melodyTimer / 0.26f) * 2.0 * kotlin.math.PI * freq.toDouble()).toFloat()
        val envelope = 1f - melodyTimer / 0.26f
        value += sin(notePhase) * 0.03f * envelope
        if (melodyIndex % 4 == 0) {
            value += sin(notePhase * 0.5f) * 0.035f * envelope
        }

        val it = effects.iterator()
        while (it.hasNext()) {
            val effect = it.next()
            effect.age += sampleDt
            if (effect.age >= effect.duration) {
                it.remove()
                continue
            }
            value += effectSample(effect)
        }
        return value.coerceIn(-1f, 1f)
    }

    private fun effectSample(e: Effect): Float {
        val t = e.age
        val d = e.duration
        val phase = (2.0 * kotlin.math.PI * t.toDouble()).toFloat()
        val sample: Float = when (e.kind) {
            Sfx.STING -> sin(phase * (950f - t * 2200f)) * (1f - t / d) * 0.38f
            Sfx.POLLEN -> (sin(phase * 1320f) + sin(phase * 1980f) * 0.5f) * (1f - t / d) * 0.25f
            Sfx.DASH -> (sin(phase * (260f + t * 1400f)) + noise() * 0.25f) * (1f - t / d) * 0.3f
            Sfx.EXPLODE -> (noise() * 0.75f + sin(phase * 90f) * 0.35f) * (1f - t / d) * 0.5f
            Sfx.HIT -> noise() * (1f - t / d) * 0.22f
            Sfx.WAVE -> {
                val notes = floatArrayOf(523f, 659f, 784f, 1046f)
                val step = (t / d * 4f).toInt().coerceIn(0, 3)
                val wavePhase = (2.0 * kotlin.math.PI * notes[step].toDouble() * t.toDouble()).toFloat()
                sin(wavePhase) * (1f - t / d) * 0.28f
            }
            Sfx.ULT -> (sin(phase * (180f + t * 900f)) + noise() * 0.4f) * (1f - t / d) * 0.42f
            Sfx.CLICK -> sin(phase * 780f) * (1f - t / d) * 0.22f
            Sfx.BOSS -> (sin(phase * 82f) + sin(phase * 123f)) * (1f - t / d) * 0.3f
        }
        return sample
    }

    private fun noise(): Float {
        noiseSeed = (noiseSeed * 1103515245 + 12345) and 0x7fffffff
        return (noiseSeed / 1073741823f) - 1f
    }
}
