package com.example.model

import android.graphics.Bitmap

enum class LayerType {
    BACKGROUND,
    CUTOUT_OBJECT,
    TEXT,
    STICKER,
    DRAWING
}

data class PointD(val x: Float, val y: Float)

data class PhotoLayer(
    val id: String,
    val name: String,
    val bitmap: Bitmap? = null,
    val type: LayerType = LayerType.CUTOUT_OBJECT,
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var alpha: Float = 1f,
    var isVisible: Boolean = true,
    var text: String = "",
    var textColor: Long = 0xFFFFFFFF,
    var textSize: Float = 48f
)

enum class CutoutTool {
    NONE,
    FREEHAND_LASSO,
    POLYGON_LASSO,
    MAGIC_WAND,
    RECT_SELECT
}

enum class StudioFilter {
    NORMAL,
    VIVID_POP,
    CYBERPUNK,
    GOLDEN_HOUR,
    NOIR,
    EMERALD_MINT,
    RETRO_SUNSET
}

data class ColorAdjustments(
    val brightness: Float = 0f,   // -100 to 100
    val contrast: Float = 1f,     // 0.5 to 2.0
    val saturation: Float = 1f,   // 0.0 to 2.0
    val warmth: Float = 0f,       // -50 to 50
    val vignette: Float = 0f      // 0.0 to 1.0
)
