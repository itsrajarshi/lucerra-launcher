package dev.rajarshi.glasshome

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.util.concurrent.Executors
import kotlin.random.Random

/** Whether the heavy visuals are on. Flipped by the dock toggle or by system "animations off". */
object Fx {
    var reduced = false
}

/**
 * Procedural wallpapers, so there is nothing to license and the APK stays tiny. Each wallpaper is
 * rendered once at 960x540 plus a tiny pre-blurred copy; glass panels sample that copy, so there is
 * no per-frame blur on a weak GPU.
 */
object Wallpapers {
    private val palettes = arrayOf(
        intArrayOf(0xFF0B1026.toInt(), 0xFF3B5BDB.toInt(), 0xFF9C36B5.toInt(), 0xFFFF8787.toInt()),
        intArrayOf(0xFF081C15.toInt(), 0xFF2D6A4F.toInt(), 0xFF40916C.toInt(), 0xFFB7E4C7.toInt()),
        intArrayOf(0xFF1A0B2E.toInt(), 0xFF7209B7.toInt(), 0xFFF72585.toInt(), 0xFF4CC9F0.toInt()),
        intArrayOf(0xFF1B1B2F.toInt(), 0xFFE76F51.toInt(), 0xFFF4A261.toInt(), 0xFF264653.toInt()),
        intArrayOf(0xFF03121F.toInt(), 0xFF0077B6.toInt(), 0xFF00B4D8.toInt(), 0xFF90E0EF.toInt()),
    )

    var cur: Bitmap? = null; private set
    var prev: Bitmap? = null; private set
    var curBlur: Bitmap? = null; private set
    var prevBlur: Bitmap? = null; private set
    var fade = 1f; private set

    private val views = HashSet<View>()
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var index = Random.nextInt(palettes.size)
    private var fader: ValueAnimator? = null

    fun attach(v: View) { views.add(v) }
    fun detach(v: View) { views.remove(v) }

    private var assets: AssetManager? = null
    private var photos: List<String> = emptyList()

    /** Picks up 1080p scenery from assets/wallpapers; with none there, falls back to generated art. */
    fun init(ctx: Context) {
        assets = ctx.applicationContext.assets
        photos = (assets!!.list("wallpapers") ?: emptyArray())
            .filter { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") }
            .shuffled()
    }

    /** Render the next wallpaper off the main thread, then crossfade to it. */
    fun next(animate: Boolean) {
        val i = index++
        worker.execute {
            val sharp = photos.takeIf { it.isNotEmpty() }?.let { decode(it[i % it.size]) }
                ?: render(palettes[i % palettes.size], System.nanoTime())
            val blur = blur(sharp)
            main.post { swapIn(sharp, blur, animate) }
        }
    }

    /** Full 1920x1080, no downscale: the photo is the whole point of the screen. */
    private fun decode(name: String): Bitmap? = try {
        assets!!.open("wallpapers/$name").use { BitmapFactory.decodeStream(it) }?.let(::cropTo169)
    } catch (e: Exception) {
        null
    }

    /** Centre-crop so photos of any aspect fill 16:9 without stretching. */
    private fun cropTo169(b: Bitmap): Bitmap {
        val h = minOf(b.height, b.width * 9 / 16)
        val w = h * 16 / 9
        if (w == b.width && h == b.height) return b
        return Bitmap.createBitmap(b, (b.width - w) / 2, (b.height - h) / 2, w, h).also { b.recycle() }
    }

    private fun swapIn(sharp: Bitmap, blur: Bitmap, animate: Boolean) {
        fader?.cancel()
        prev = cur; prevBlur = curBlur
        cur = sharp; curBlur = blur
        if (!animate || prev == null) {
            prev = null; prevBlur = null; fade = 1f
            invalidateViews()
            return
        }
        fade = 0f
        fader = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600
            interpolator = DecelerateInterpolator()
            addUpdateListener { fade = it.animatedValue as Float; invalidateViews() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) { prev = null; prevBlur = null }
            })
            start()
        }
    }

    private fun invalidateViews() = views.forEach { it.postInvalidateOnAnimation() }

    private fun render(p: IntArray, seed: Long): Bitmap {
        val w = 960
        val h = 540
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = Random(seed)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

        paint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), p[0], 0xFF000000.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Big soft glows.
        repeat(5) { i ->
            val col = p[1 + i % 3]
            val cx = r.nextFloat() * w
            val cy = r.nextFloat() * h
            val rad = h * (0.4f + r.nextFloat() * 0.5f)
            paint.shader = RadialGradient(
                cx, cy, rad,
                intArrayOf(alpha(col, 0xE6), alpha(col, 0xA0), alpha(col, 0x00)),
                floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP,
            )
            c.drawCircle(cx, cy, rad, paint)
        }

        // A few crisp translucent slabs, so the frosted glass has edges to blur.
        paint.shader = null
        repeat(3) { i ->
            paint.color = alpha(p[1 + i % 3], 0x38)
            c.save()
            c.rotate(-18f + r.nextFloat() * 36f, w / 2f, h / 2f)
            val left = r.nextFloat() * w * 0.7f
            val top = r.nextFloat() * h * 0.7f
            c.drawRoundRect(left, top, left + w * 0.45f, top + h * 0.5f, 90f, 90f, paint)
            c.restore()
        }
        return bmp
    }

    /** Repeated halving with bilinear filtering; about 60x34, plenty for a frosted look. */
    private fun blur(src: Bitmap): Bitmap {
        var b = src
        while (b.width > 60) {
            val next = Bitmap.createScaledBitmap(b, b.width / 2, b.height / 2, true)
            if (b !== src) b.recycle()
            b = next
        }
        return b
    }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)
}
