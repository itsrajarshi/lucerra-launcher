package dev.rajarshi.glasshome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Liquid-glass surface for Android 9 (no RenderEffect): paints its own slice of the pre-blurred
 * wallpaper, boosts saturation, adds a translucent tint, and a light-catching rim that is brighter
 * on the top-left. Falls back to an opaque panel when effects are reduced.
 */
class GlassFrame @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : FrameLayout(ctx, attrs) {
    private val density = resources.displayMetrics.density
    private var radius = 22f * density
    private val path = Path()
    private val rect = RectF()
    private val loc = IntArray(2)
    private val src = Rect()
    private val dst = Rect()

    private val shaderMatrix = Matrix()
    private val slice = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.35f) })
    }
    private val tint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val solid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF01C2030.toInt() }
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }

    init { setWillNotDraw(false) }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); Wallpapers.attach(this) }
    override fun onDetachedFromWindow() { Wallpapers.detach(this); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        // small pills become true capsules; a full-width surface (the All Apps screen) is square
        radius = if (w >= resources.displayMetrics.widthPixels) 0f else minOf(22f * density, h / 2f)
        val inset = rim.strokeWidth / 2
        rect.set(inset, inset, w - inset, h - inset)
        path.reset()
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)
        dst.set(0, 0, w, h)
        tint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0x3DFFFFFF, 0x1FFFFFFF), null, Shader.TileMode.CLAMP,
        )
        rim.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xCCFFFFFF.toInt(), 0x1AFFFFFF, 0x66FFFFFF), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        val blur = Wallpapers.curBlur
        if (Fx.reduced || blur == null) {
            canvas.drawPath(path, solid)
        } else {
            getLocationOnScreen(loc)
            Wallpapers.prevBlur?.let { drawSlice(canvas, it, 255) }
            drawSlice(canvas, blur, (Wallpapers.fade * 255).toInt())
            canvas.drawPath(path, tint)
        }
        canvas.drawPath(path, rim)
    }

    private fun drawSlice(canvas: Canvas, bmp: Bitmap, a: Int) {
        val sw = rootView.width.coerceAtLeast(1)
        val sh = rootView.height.coerceAtLeast(1)
        src.set(
            loc[0] * bmp.width / sw,
            loc[1] * bmp.height / sh,
            (loc[0] + width) * bmp.width / sw,
            (loc[1] + height) * bmp.height / sh,
        )
        // Filled through the path (anti-aliased) rather than clipped: a hardware clip left a 1px seam on the capsule edge.
        shaderMatrix.setRectToRect(RectF(src), RectF(dst), Matrix.ScaleToFit.FILL)
        slice.shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also { it.setLocalMatrix(shaderMatrix) }
        slice.alpha = a
        canvas.drawPath(path, slice)
    }
}
