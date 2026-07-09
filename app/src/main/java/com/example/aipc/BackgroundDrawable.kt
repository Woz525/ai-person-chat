package com.example.aipc

import android.graphics.*
import android.graphics.drawable.Drawable

class BackgroundDrawable(private val bitmap: Bitmap?) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        bitmap?.let { bmp ->
            val srcRect = Rect(0, 0, bmp.width, bmp.height)
            val dstWidth = canvas.width
            val dstHeight = canvas.height
            // ★ 修正：使用 maxOf 替代 max
            val scale = maxOf(dstWidth / bmp.width.toFloat(), dstHeight / bmp.height.toFloat())
            val newWidth = (bmp.width * scale).toInt()
            val newHeight = (bmp.height * scale).toInt()
            val dx = (dstWidth - newWidth) / 2
            val dy = (dstHeight - newHeight) / 2
            val dstRect = Rect(dx, dy, dx + newWidth, dy + newHeight)
            canvas.drawBitmap(bmp, srcRect, dstRect, paint)
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}