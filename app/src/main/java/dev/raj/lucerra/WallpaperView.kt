package dev.raj.lucerra

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
    private val dst = RectF()

    override fun onAttachedToWindow() { super.onAttachedToWindow(); Wallpapers.attach(this) }
    override fun onDetachedFromWindow() { Wallpapers.detach(this); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { dst.set(0f, 0f, w.toFloat(), h.toFloat()) }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(0xFF0B0F14.toInt())
        Wallpapers.prev?.let { canvas.drawBitmap(it, null, dst, paint) }
        Wallpapers.cur?.let {
            paint.alpha = (Wallpapers.fade * 255).toInt()
            canvas.drawBitmap(it, null, dst, paint)
            paint.alpha = 255
        }
    }
}
