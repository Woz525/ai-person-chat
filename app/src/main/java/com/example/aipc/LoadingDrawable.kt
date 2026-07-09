package com.example.aipc

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper

class LoadingDrawable(private val text: String) : Drawable() {

    private var dotCount = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF") // 半透明白色，柔和
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            dotCount = (dotCount + 1) % 4  // 0,1,2,3 对应 "", ".", "..", "..."
            invalidateSelf()
            handler.postDelayed(this, 500)
        }
    }

    init {
        handler.post(updateRunnable)
    }

    override fun draw(canvas: Canvas) {
        val dots = when (dotCount) {
            0 -> ""
            1 -> "."
            2 -> ".."
            else -> "..."
        }
        val displayText = "$text$dots"
        val bounds = canvas.clipBounds
        val x = bounds.centerX().toFloat()
        val y = bounds.centerY().toFloat() - paint.descent() / 2 + paint.ascent() / 2
        canvas.drawText(displayText, x, y, paint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    fun stop() {
        handler.removeCallbacks(updateRunnable)
    }
}