package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.SampleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object SynthesizerEngine {
    private const val SAMPLE_RATE = 44100
    private val sampleCache = ConcurrentHashMap<SampleType, ShortArray>()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        // Pre-generate standard drum samples into memory on startup
        scope.launch {
            SampleType.values().forEach { sampleType ->
                sampleCache[sampleType] = generatePcm(sampleType)
            }
        }
    }

    private fun generatePcm(type: SampleType): ShortArray {
        return when (type) {
            SampleType.KICK -> generateKick()
            SampleType.SNARE -> generateSnare()
            SampleType.HIHAT_CLOSED -> generateHiHat(isOpen = false)
            SampleType.HIHAT_OPEN -> generateHiHat(isOpen = true)
            SampleType.CLAP -> generateClap()
            SampleType.SUB_808 -> generateSub808()
            SampleType.SYNTH_PLUCK -> generatePluck(440f)
            SampleType.PERC_DROP -> generatePercDrop()
        }
    }

    private fun generateKick(): ShortArray {
        val durationSec = 0.28f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        val startFreq = 160.0
        val endFreq = 42.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            // Exponential pitch drop
            val instantFreq = endFreq + (startFreq - endFreq) * exp(-progress * 14.0)
            val phase = 2.0 * PI * instantFreq * t
            // Punchy amplitude envelope
            val amp = exp(-progress * 8.0) * (1.0 - progress)
            val sample = (sin(phase) * amp * Short.MAX_VALUE * 0.95).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generateSnare(): ShortArray {
        val durationSec = 0.22f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        val toneFreq = 185.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            val noise = Random.nextDouble(-1.0, 1.0)
            val tone = sin(2.0 * PI * toneFreq * t) * exp(-progress * 18.0)
            val noiseAmp = exp(-progress * 10.0)
            val mixed = (noise * 0.7 * noiseAmp + tone * 0.3)
            val sample = (mixed * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generateHiHat(isOpen: Boolean): ShortArray {
        val durationSec = if (isOpen) 0.32f else 0.055f
        val decayRate = if (isOpen) 10.0 else 45.0
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            // High frequency metallic noise
            val metallic = (sin(2.0 * PI * 8000.0 * t) + sin(2.0 * PI * 9500.0 * t) + Random.nextDouble(-1.0, 1.0) * 2.0) / 3.0
            val amp = exp(-progress * decayRate)
            val sample = (metallic * amp * Short.MAX_VALUE * 0.75).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generateClap(): ShortArray {
        val durationSec = 0.24f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            val noise = Random.nextDouble(-1.0, 1.0)
            // Multi-tap attack
            val tapAmp = when {
                t < 0.015 -> 0.4
                t < 0.030 -> 0.7
                t < 0.045 -> 1.0
                else -> exp(-(progress - 0.045) * 12.0)
            }
            val sample = (noise * tapAmp * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generateSub808(): ShortArray {
        val durationSec = 0.65f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        val freq = 46.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            val fundamental = sin(2.0 * PI * freq * t)
            val harmonic = sin(2.0 * PI * freq * 2.0 * t) * 0.25
            // Soft saturation curve
            val raw = fundamental + harmonic
            val saturated = raw / (1.0 + Math.abs(raw) * 0.5)
            val amp = (1.0 - progress * 0.7)
            val sample = (saturated * amp * Short.MAX_VALUE * 0.9).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generatePluck(baseFreq: Float): ShortArray {
        val durationSec = 0.45f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            val s1 = sin(2.0 * PI * baseFreq * t)
            val s2 = sin(2.0 * PI * (baseFreq * 1.5) * t) * 0.5
            val s3 = sin(2.0 * PI * (baseFreq * 2.0) * t) * 0.25
            val amp = exp(-progress * 8.0)
            val sample = ((s1 + s2 + s3) * amp * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generatePercDrop(): ShortArray {
        val durationSec = 0.20f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            val freq = 900.0 * exp(-progress * 8.0) + 120.0
            val wave = sin(2.0 * PI * freq * t)
            val amp = exp(-progress * 11.0)
            val sample = (wave * amp * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    fun playSample(type: SampleType, volume: Float = 1.0f) {
        scope.launch(Dispatchers.IO) {
            val pcm = sampleCache[type] ?: generatePcm(type).also { sampleCache[type] = it }
            playPcm(pcm, volume)
        }
    }

    private fun playPcm(data: ShortArray, volume: Float) {
        try {
            val adjusted = if (volume < 0.99f) {
                ShortArray(data.size) { (data[it] * volume.coerceIn(0f, 1f)).toInt().toShort() }
            } else {
                data
            }

            val bufferSize = adjusted.size * 2
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(adjusted, 0, adjusted.size)
            audioTrack.play()
            // Release after playback finishes
            scope.launch {
                val delayMs = (adjusted.size.toLong() * 1000L / SAMPLE_RATE) + 100L
                kotlinx.coroutines.delay(delayMs)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            // Audio track failure handled gracefully
        }
    }
}
