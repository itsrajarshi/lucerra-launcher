package dev.rajarshi.glasshome

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class WallpaperView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val scrim = Paint()
    private val dst = RectF()

    override fun onAttachedToWindow() { super.onAttachedToWindow(); Wallpapers.attach(this) }
    override fun onDetachedFromWindow() { Wallpapers.detach(this); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        dst.set(0f, 0f, w.toFloat(), h.toFloat())
        // Darken top and bottom so clock and dock stay legible on any wallpaper.
        scrim.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x800B0F14.toInt(), 0x000B0F14, 0x000B0F14, 0xA60B0F14.toInt()),
            floatArrayOf(0f, 0.3f, 0.6f, 1f), Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(0xFF0B0F14.toInt())
        Wallpapers.prev?.let { canvas.drawBitmap(it, null, dst, paint) }
        Wallpapers.cur?.let {
            paint.alpha = (Wallpapers.fade * 255).toInt()
            canvas.drawBitmap(it, null, dst, paint)
            paint.alpha = 255
        }
        canvas.drawRect(dst, scrim)
    }
}
