package com.example.model

enum class AspectRatioType(val label: String, val ratio: Float) {
    LANDSCAPE("16:9 Cinema", 16f / 9f),
    PORTRAIT("9:16 Shorts/Reel", 9f / 16f),
    SQUARE("1:1 Feed", 1f),
    CLASSIC("4:5 Post", 4f / 5f)
}

data class VideoClip(
    val id: String,
    val title: String,
    val totalDurationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = totalDurationMs,
    val speed: Float = 1.0f,
    val filter: StudioFilter = StudioFilter.NORMAL,
    val accentColor: Long = 0xFF1976D2,
    val sceneName: String = "Scene"
) {
    val activeDurationMs: Long
        get() = ((trimEndMs - trimStartMs).coerceAtLeast(100L) / speed).toLong()
}

data class VideoOverlay(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val isWatermark: Boolean = false
)
