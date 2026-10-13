package com.example.model

enum class SampleType(val displayName: String, val baseFreq: Float, val colorHex: Long) {
    KICK("808 KICK", 60f, 0xFFFF5252),
    SNARE("CRISP SNARE", 220f, 0xFF00E5FF),
    HIHAT_CLOSED("CLOSED HAT", 1200f, 0xFFFFB300),
    HIHAT_OPEN("OPEN HAT", 1000f, 0xFFFFD54F),
    CLAP("STUDIO CLAP", 350f, 0xFFFF4081),
    SUB_808("SUB 808", 45f, 0xFFB388FF),
    SYNTH_PLUCK("CHORD PLUCK", 440f, 0xFF00E676),
    PERC_DROP("VINYL DROP", 700f, 0xFF64B5F6)
}

data class SequencerChannel(
    val id: String,
    val sampleType: SampleType,
    val steps: List<Boolean> = List(16) { false }
)

data class StudioAudioTrack(
    val id: String,
    val name: String,
    val volume: Float = 0.8f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val colorHex: Long = 0xFF00E5FF,
    val waveData: List<Float> = emptyList(),
    val durationSec: Float = 4.0f
)
