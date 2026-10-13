package com.example.model

enum class StudioType {
    PHOTO,
    AUDIO,
    VIDEO
}

data class ProjectItem(
    val id: String,
    val title: String,
    val type: StudioType,
    val updatedAt: Long = System.currentTimeMillis(),
    val description: String = "",
    val durationOrLayers: String = ""
)
