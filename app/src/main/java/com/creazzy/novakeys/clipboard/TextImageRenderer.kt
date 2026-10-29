package com.creazzy.novakeys.clipboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.graphics.Typeface

/**
 * Renders user-provided text locally, using Android's system font fallback so Unicode
 * emoji and mixed Arabic/Latin text are drawn by the device rather than sent to a server.
 */
object TextImageRenderer {
    private const val IMAGE_WIDTH = 1080
    private const val MAX_TEXT_LENGTH = 6000

    fun render(rawText: String): Bitmap {
        val text = rawText.trimEnd()
        require(text.isNotBlank()) { "Copy or enter some text first" }
        require(text.length <= MAX_TEXT_LENGTH) {
            "This text is too long for one image. Split it into shorter parts (up to $MAX_TEXT_LENGTH characters)."
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.rgb(34, 43, 58)
            textSize = 44f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isSubpixelText = true
        }
        val contentWidth = IMAGE_WIDTH - 168
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setIncludePad(true)
            .setLineSpacing(9f, 1.12f)
            .build()

        require(layout.height <= 7600) {
            "This text would create a very tall image. Split it into shorter parts."
        }
        val imageHeight = (layout.height + 212).coerceAtLeast(380)
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, imageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, IMAGE_WIDTH.toFloat(), imageHeight.toFloat(),
                Color.rgb(237, 243, 250), Color.rgb(248, 242, 235), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), imageHeight.toFloat(), background)

        val card = RectF(28f, 28f, IMAGE_WIDTH - 28f, imageHeight - 28f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(card, 30f, 30f, cardPaint)

        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(39, 104, 166) }
        canvas.drawRoundRect(RectF(64f, 58f, 76f, imageHeight - 58f), 6f, 6f, accent)

        canvas.save()
        canvas.translate(100f, 82f)
        layout.draw(canvas)
        canvas.restore()

        val footerY = imageHeight - 62f
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 2f
        }
        canvas.drawLine(100f, footerY - 30f, IMAGE_WIDTH - 70f, footerY - 30f, divider)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 119, 137)
            textSize = 24f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText("NovaKeys  ·  Text to Image", 100f, footerY + 8f, footerPaint)
        return bitmap
    }
}
