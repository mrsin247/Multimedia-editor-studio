package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.model.ProjectItem
import com.example.model.StudioType

object SampleMediaRepository {

    fun getStarterProjects(): List<ProjectItem> {
        return listOf(
            ProjectItem(
                id = "demo_photo_1",
                title = "Cyberpunk Skyline Cutout",
                type = StudioType.PHOTO,
                updatedAt = System.currentTimeMillis() - 1000 * 60 * 15,
                description = "Lasso & Multi-layer asset extraction",
                durationOrLayers = "4 Layers"
            ),
            ProjectItem(
                id = "demo_audio_1",
                title = "Midnight Trap 140 BPM",
                type = StudioType.AUDIO,
                updatedAt = System.currentTimeMillis() - 1000 * 60 * 60,
                description = "16-step 808 beat sequencer & pads",
                durationOrLayers = "140 BPM • 4 Tracks"
            ),
            ProjectItem(
                id = "demo_video_1",
                title = "Cinematic Reel Sequence",
                type = StudioType.VIDEO,
                updatedAt = System.currentTimeMillis() - 1000 * 60 * 120,
                description = "9:16 Shorts timeline with overlays",
                durationOrLayers = "0:18 • 3 Clips"
            )
        )
    }

    /**
     * Generates vibrant high-res procedural starter canvases so users can test lasso cutting immediately.
     */
    fun createSamplePhoto(templateIndex: Int): Bitmap {
        val width = 800
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (templateIndex % 3) {
            0 -> {
                // Cyberpunk Neon City
                val skyGradient = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(15, 12, 41), Color.rgb(48, 43, 99), Color.rgb(36, 36, 62)),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = skyGradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Glowing Neon Moon (Asset to cut out!)
                val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val moonGradient = RadialGradient(
                    width * 0.75f, height * 0.28f, 120f,
                    intArrayOf(Color.rgb(0, 240, 255), Color.rgb(255, 0, 128), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.7f, 1f),
                    Shader.TileMode.CLAMP
                )
                moonPaint.shader = moonGradient
                canvas.drawCircle(width * 0.75f, height * 0.28f, 110f, moonPaint)

                // Neon Skyscrapers
                paint.color = Color.rgb(18, 18, 30)
                canvas.drawRect(80f, 420f, 240f, height.toFloat(), paint)
                canvas.drawRect(260f, 320f, 460f, height.toFloat(), paint)
                canvas.drawRect(480f, 480f, 620f, height.toFloat(), paint)
                canvas.drawRect(640f, 380f, 760f, height.toFloat(), paint)

                // Lit windows
                val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                windowPaint.color = Color.rgb(0, 229, 255)
                for (r in 350..700 step 40) {
                    canvas.drawRect(290f, r.toFloat(), 330f, (r + 15).toFloat(), windowPaint)
                    canvas.drawRect(370f, r.toFloat(), 410f, (r + 15).toFloat(), windowPaint)
                }

                // Floating Hero Gem / Crystal (Prime Cutout Target!)
                val crystalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                crystalPaint.color = Color.rgb(255, 64, 129)
                val gemPath = Path()
                val cx = width * 0.35f
                val cy = height * 0.55f
                gemPath.moveTo(cx, cy - 90f)
                gemPath.lineTo(cx + 70f, cy)
                gemPath.lineTo(cx, cy + 90f)
                gemPath.lineTo(cx - 70f, cy)
                gemPath.close()
                canvas.drawPath(gemPath, crystalPaint)
            }
            1 -> {
                // Retro Synthwave Sunset
                val sunGradient = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(255, 94, 98), Color.rgb(255, 153, 102), Color.rgb(44, 62, 80)),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = sunGradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Striped Giant Sun (Cutout asset!)
                val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                sunPaint.color = Color.rgb(255, 235, 59)
                canvas.drawCircle(width * 0.5f, height * 0.45f, 150f, sunPaint)

                // Sun horizontal stripes
                val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                stripePaint.color = Color.rgb(255, 120, 100)
                for (y in (height * 0.45f).toInt()..(height * 0.60f).toInt() step 18) {
                    canvas.drawRect(width * 0.25f, y.toFloat(), width * 0.75f, (y + 6).toFloat(), stripePaint)
                }

                // Retro Mountain peaks
                val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                mountainPaint.color = Color.rgb(26, 26, 46)
                val mPath = Path()
                mPath.moveTo(0f, height * 0.75f)
                mPath.lineTo(200f, height * 0.55f)
                mPath.lineTo(380f, height * 0.70f)
                mPath.lineTo(550f, height * 0.50f)
                mPath.lineTo(width.toFloat(), height * 0.75f)
                mPath.lineTo(width.toFloat(), height.toFloat())
                mPath.lineTo(0f, height.toFloat())
                mPath.close()
                canvas.drawPath(mPath, mountainPaint)
            }
            else -> {
                // Golden Studio Horizon with Floating Sphere
                val studioGrad = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.rgb(33, 37, 41), Color.rgb(52, 58, 64), Color.rgb(18, 18, 18)),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = studioGrad
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Floating Golden Sphere with specular highlight
                val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val sphereGrad = RadialGradient(
                    width * 0.45f, height * 0.42f, 130f,
                    intArrayOf(Color.rgb(255, 236, 179), Color.rgb(255, 179, 0), Color.rgb(191, 54, 12)),
                    null, Shader.TileMode.CLAMP
                )
                spherePaint.shader = sphereGrad
                canvas.drawCircle(width * 0.5f, height * 0.48f, 120f, spherePaint)

                // Golden Ring / Torus around sphere
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                ringPaint.color = Color.rgb(0, 229, 255)
                ringPaint.style = Paint.Style.STROKE
                ringPaint.strokeWidth = 14f
                canvas.drawOval(width * 0.25f, height * 0.45f, width * 0.75f, height * 0.55f, ringPaint)
            }
        }

        return bitmap
    }
}
