package com.example.nokiacam.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object ImageProcessor {

    fun decode(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("Unable to decode captured image")
    }

    fun applyMode(bitmap: Bitmap, mode: CameraMode): Bitmap {
        return when (mode) {
            CameraMode.NOKIA -> applyNokia(bitmap)
            CameraMode.RETRO -> applyRetro(bitmap)
        }
    }

    private fun applyNokia(source: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val matrix = ColorMatrix().apply {
            setSaturation(1.08f)
        }
        val contrast = ColorMatrix(
            floatArrayOf(
                1.08f, 0f, 0f, 0f, -6f,
                0f, 1.08f, 0f, 0f, -6f,
                0f, 0f, 1.08f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(contrast)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    private fun applyRetro(source: Bitmap): Bitmap {
        val targetWidth = min(source.width, 1280)
        val scale = targetWidth.toFloat() / source.width.toFloat()
        val targetHeight = max(1, (source.height * scale).toInt())
        val small = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        val base = Bitmap.createScaledBitmap(small, source.width, source.height, false)
        if (small !== base) small.recycle()

        val out = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val sepia = ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 12f,
                0.349f, 0.686f, 0.168f, 0f, 8f,
                0.272f, 0.534f, 0.131f, 0f, -8f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val fade = ColorMatrix().apply { setSaturation(0.72f) }
        sepia.postConcat(fade)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(sepia)
        }
        canvas.drawBitmap(base, 0f, 0f, paint)
        base.recycle()

        addNoise(out, 18)
        addVignette(out)
        addTimestamp(out)
        return out
    }

    private fun addNoise(bitmap: Bitmap, strength: Int) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val rnd = Random(7)
        for (i in pixels.indices step 2) {
            val n = rnd.nextInt(-strength, strength + 1)
            val c = pixels[i]
            val a = Color.alpha(c)
            val r = (Color.red(c) + n).coerceIn(0, 255)
            val g = (Color.green(c) + n).coerceIn(0, 255)
            val b = (Color.blue(c) + n).coerceIn(0, 255)
            pixels[i] = Color.argb(a, r, g, b)
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun addVignette(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val radius = max(bitmap.width, bitmap.height) * 0.78f
        val paint = Paint().apply {
            shader = android.graphics.RadialGradient(
                bitmap.width / 2f,
                bitmap.height / 2f,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(90, 0, 0, 0)),
                floatArrayOf(0.55f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
    }

    private fun addTimestamp(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val text = "00:01  01.01.2004"
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 170, 60)
            textSize = bitmap.width * 0.045f
            style = Paint.Style.FILL
            setShadowLayer(4f, 2f, 2f, Color.argb(160, 0, 0, 0))
        }
        val x = bitmap.width * 0.06f
        val y = bitmap.height * 0.93f
        canvas.drawText(text, x, y, paint)
    }
}
