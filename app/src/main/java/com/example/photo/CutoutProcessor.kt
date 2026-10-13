package com.example.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.example.model.ColorAdjustments
import com.example.model.PointD
import com.example.model.StudioFilter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object CutoutProcessor {

    /**
     * Extracts an object from a source bitmap using a closed polygon or freehand lasso path.
     * Produces a standalone transparent Bitmap cropped tightly around the extracted object.
     */
    fun extractLassoAsset(
        source: Bitmap,
        points: List<PointD>,
        viewWidth: Float,
        viewHeight: Float
    ): Pair<Bitmap, RectF>? {
        if (points.size < 3) return null

        // Scale factors from viewport to original bitmap pixels
        val scaleX = source.width.toFloat() / viewWidth.coerceAtLeast(1f)
        val scaleY = source.height.toFloat() / viewHeight.coerceAtLeast(1f)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        val scaledPoints = points.map { pt ->
            val sx = (pt.x * scaleX).coerceIn(0f, source.width.toFloat())
            val sy = (pt.y * scaleY).coerceIn(0f, source.height.toFloat())
            minX = min(minX, sx)
            minY = min(minY, sy)
            maxX = max(maxX, sx)
            maxY = max(maxY, sy)
            PointD(sx, sy)
        }

        val boxWidth = (maxX - minX).toInt().coerceAtLeast(10)
        val boxHeight = (maxY - minY).toInt().coerceAtLeast(10)

        // Create transparent result bitmap for the extracted object
        val extracted = Bitmap.createBitmap(boxWidth, boxHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(extracted)

        // Build path relative to the bounding box
        val path = Path()
        val first = scaledPoints.first()
        path.moveTo(first.x - minX, first.y - minY)
        for (i in 1 until scaledPoints.size) {
            val pt = scaledPoints[i]
            path.lineTo(pt.x - minX, pt.y - minY)
        }
        path.close()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        // Draw the mask path
        canvas.drawPath(path, paint)

        // PorterDuff SRC_IN mode keeps only the source bitmap within the drawn path
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = Rect(minX.toInt(), minY.toInt(), (minX + boxWidth).toInt(), (minY + boxHeight).toInt())
        val dstRect = Rect(0, 0, boxWidth, boxHeight)
        canvas.drawBitmap(source, srcRect, dstRect, paint)

        val boundsInView = RectF(
            minX / scaleX,
            minY / scaleY,
            maxX / scaleX,
            maxY / scaleY
        )

        return Pair(extracted, boundsInView)
    }

    /**
     * Magic Wand / Color-Key segmentation:
     * Selects pixels matching the seed coordinate color within a tolerance threshold.
     */
    fun extractColorKeyAsset(
        source: Bitmap,
        tapX: Float,
        tapY: Float,
        tolerance: Float = 45f
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val clampX = tapX.toInt().coerceIn(0, width - 1)
        val clampY = tapY.toInt().coerceIn(0, height - 1)
        val targetColor = source.getPixel(clampX, clampY)

        val tR = Color.red(targetColor)
        val tG = Color.green(targetColor)
        val tB = Color.blue(targetColor)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)

            val dist = sqrt(((r - tR) * (r - tR) + (g - tG) * (g - tG) + (b - tB) * (b - tB)).toDouble())
            if (dist <= tolerance) {
                // Keep selected subject
                outPixels[i] = c
            } else {
                // Transparent
                outPixels[i] = 0
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Inpainting patch algorithm:
     * Fills the hole created when an object was extracted by diffusing surrounding border colors.
     */
    fun inpaintHole(
        source: Bitmap,
        holeRect: Rect
    ): Bitmap {
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true)
        val left = holeRect.left.coerceIn(0, mutable.width - 1)
        val top = holeRect.top.coerceIn(0, mutable.height - 1)
        val right = holeRect.right.coerceIn(0, mutable.width - 1)
        val bottom = holeRect.bottom.coerceIn(0, mutable.height - 1)

        val borderLeft = (left - 5).coerceAtLeast(0)
        val borderRight = (right + 5).coerceAtMost(mutable.width - 1)
        val borderTop = (top - 5).coerceAtLeast(0)
        val borderBottom = (bottom + 5).coerceAtMost(mutable.height - 1)

        // Sample border average colors
        val avgColorLeft = mutable.getPixel(borderLeft, (top + bottom) / 2)
        val avgColorRight = mutable.getPixel(borderRight, (top + bottom) / 2)
        val avgColorTop = mutable.getPixel((left + right) / 2, borderTop)
        val avgColorBottom = mutable.getPixel((left + right) / 2, borderBottom)

        for (y in top..bottom) {
            val yWeight = if (bottom > top) (y - top).toFloat() / (bottom - top) else 0.5f
            for (x in left..right) {
                val xWeight = if (right > left) (x - left).toFloat() / (right - left) else 0.5f

                val r = ((Color.red(avgColorLeft) * (1 - xWeight) + Color.red(avgColorRight) * xWeight +
                        Color.red(avgColorTop) * (1 - yWeight) + Color.red(avgColorBottom) * yWeight) / 2f).toInt().coerceIn(0, 255)
                val g = ((Color.green(avgColorLeft) * (1 - xWeight) + Color.green(avgColorRight) * xWeight +
                        Color.green(avgColorTop) * (1 - yWeight) + Color.green(avgColorBottom) * yWeight) / 2f).toInt().coerceIn(0, 255)
                val b = ((Color.blue(avgColorLeft) * (1 - xWeight) + Color.blue(avgColorRight) * xWeight +
                        Color.blue(avgColorTop) * (1 - yWeight) + Color.blue(avgColorBottom) * yWeight) / 2f).toInt().coerceIn(0, 255)

                mutable.setPixel(x, y, Color.rgb(r, g, b))
            }
        }

        return mutable
    }

    /**
     * Builds a ColorMatrixColorFilter combining adjustments and artistic presets.
     */
    fun createCombinedFilter(
        adjustments: ColorAdjustments,
        preset: StudioFilter
    ): ColorMatrixColorFilter {
        val matrix = ColorMatrix()

        // Apply Preset base matrix
        when (preset) {
            StudioFilter.NORMAL -> { /* identity */ }
            StudioFilter.VIVID_POP -> {
                matrix.setSaturation(1.4f)
                val contrastMat = ColorMatrix(floatArrayOf(
                    1.2f, 0f, 0f, 0f, -10f,
                    0f, 1.2f, 0f, 0f, -10f,
                    0f, 0f, 1.2f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(contrastMat)
            }
            StudioFilter.CYBERPUNK -> {
                val cyberMat = ColorMatrix(floatArrayOf(
                    1.3f, 0f, 0.2f, 0f, 20f,
                    0f, 0.9f, 0f, 0f, -15f,
                    0.2f, 0f, 1.5f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(cyberMat)
            }
            StudioFilter.GOLDEN_HOUR -> {
                val goldMat = ColorMatrix(floatArrayOf(
                    1.25f, 0.1f, 0f, 0f, 30f,
                    0f, 1.1f, 0f, 0f, 15f,
                    0f, 0f, 0.8f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(goldMat)
            }
            StudioFilter.NOIR -> {
                matrix.setSaturation(0f)
                val noirContrast = ColorMatrix(floatArrayOf(
                    1.4f, 0f, 0f, 0f, -30f,
                    0f, 1.4f, 0f, 0f, -30f,
                    0f, 0f, 1.4f, 0f, -30f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(noirContrast)
            }
            StudioFilter.EMERALD_MINT -> {
                val mintMat = ColorMatrix(floatArrayOf(
                    0.9f, 0f, 0f, 0f, -10f,
                    0f, 1.35f, 0.1f, 0f, 25f,
                    0f, 0.1f, 1.2f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(mintMat)
            }
            StudioFilter.RETRO_SUNSET -> {
                val retroMat = ColorMatrix(floatArrayOf(
                    1.3f, 0f, 0f, 0f, 25f,
                    0f, 1.05f, 0f, 0f, 10f,
                    0f, 0f, 0.85f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(retroMat)
            }
        }

        // Apply Brightness
        if (adjustments.brightness != 0f) {
            val b = adjustments.brightness
            val bMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, b,
                0f, 1f, 0f, 0f, b,
                0f, 0f, 1f, 0f, b,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(bMatrix)
        }

        // Apply Contrast
        if (adjustments.contrast != 1f) {
            val c = adjustments.contrast
            val t = (1f - c) * 128f
            val cMatrix = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(cMatrix)
        }

        // Apply Saturation
        if (adjustments.saturation != 1f) {
            val sMatrix = ColorMatrix()
            sMatrix.setSaturation(adjustments.saturation)
            matrix.postConcat(sMatrix)
        }

        // Apply Warmth (Gain R and reduce B)
        if (adjustments.warmth != 0f) {
            val w = adjustments.warmth
            val wMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, w,
                0f, 1f, 0f, 0f, w * 0.3f,
                0f, 0f, 1f, 0f, -w,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(wMatrix)
        }

        return ColorMatrixColorFilter(matrix)
    }
}
