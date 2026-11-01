package dev.raj.lucerra

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
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
    @Volatile private var photos: List<String> = emptyList()

    /**
     * The bundled 1080p scenery plus the photos [WallpaperFetcher] has downloaded; with none of either,
     * falls back to generated art. Rescanned on every start. Downloaded files are stored as full paths.
     * [showNewNow] restarts the rotation on the downloaded photos, so a fresh batch shows straight away.
     */
    fun init(ctx: Context, showNewNow: Boolean = false) {
        assets = ctx.applicationContext.assets
        val bundled = (assets!!.list("wallpapers") ?: emptyArray()).filter { it.endsWith(".jpg") }.shuffled()
        val fetched = WallpaperFetcher.dir(ctx).listFiles()?.filter { it.name.endsWith(".jpg") }?.map { it.absolutePath }.orEmpty().shuffled()
        // Downloaded photos lead; the bundled six only fill in until there are enough fresh ones.
        photos = if (fetched.size >= FRESH_ENOUGH) fetched else fetched + bundled
        if (showNewNow && fetched.isNotEmpty()) { index = 0; next(animate = true) }
    }

    /** Render the next wallpaper off the main thread, then crossfade to it. */
    fun next(animate: Boolean) {
        val i = index++
        worker.execute {
            val raw = photos.takeIf { it.isNotEmpty() }?.let { decode(it[i % it.size]) }
                ?: render(palettes[i % palettes.size], System.nanoTime())
            val blur = blur(raw)
            val sharp = finish(raw)
            main.post { swapIn(sharp, blur, animate) }
        }
    }

    /** At most ~1080p: the photo is the whole point of the screen, but a 4K upload must not eat the heap. */
    private fun decode(name: String): Bitmap? = try {
        val open = { if (name.startsWith("/")) java.io.FileInputStream(name) else assets!!.open("wallpapers/$name") }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1920) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        open().use { BitmapFactory.decodeStream(it, null, opts) }?.let(::cropTo169)?.let(::darken)
    } catch (e: Exception) {
        null
    }

    /**
     * Dark-mode grade: every screen UI here is white text on glass, so bright photos are pulled down to a
     * mean luminance of about 0.24 (never lifted, never below 0.5x), and highlights are rolled off.
     */
    private fun darken(src: Bitmap): Bitmap {
        val small = Bitmap.createScaledBitmap(src, 32, 18, true)
        var sum = 0.0
        for (y in 0 until small.height) for (x in 0 until small.width) {
            val c = small.getPixel(x, y)
            sum += (0.2126 * Color.red(c) + 0.7152 * Color.green(c) + 0.0722 * Color.blue(c)) / 255.0
        }
        small.recycle()
        val mean = sum / (32 * 18)
        val k = (TARGET_LUMA / mean.coerceAtLeast(0.01)).coerceIn(0.5, 1.0).toFloat()
        if (k >= 0.995f) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setScale(k, k, k * 1.02f, 1f) // a hair cooler than neutral, which reads calmer at night
            })
        }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        src.recycle()
        return out
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

    /**
     * The one bitmap the screen draws every frame: at most 1280x720 (the TV scales it up, and the photo is graded
     * dark and soft anyway, so it costs a third less GPU bandwidth than 1080p), with the top and bottom scrim
     * baked in so the screen needs one pass instead of two.
     */
    private fun finish(src: Bitmap): Bitmap {
        val w = minOf(src.width, 1280)
        val h = w * 9 / 16
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawBitmap(src, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(0xB30B0F14.toInt(), 0x000B0F14, 0x000B0F14, 0x400B0F14, 0xB80B0F14.toInt()),
                floatArrayOf(0f, 0.26f, 0.5f, 0.68f, 1f), Shader.TileMode.CLAMP,
            )
        })
        src.recycle()
        return out
    }

    /** Repeated halving with bilinear filtering; about 60x34, plenty for a frosted look. */
    private fun blur(src: Bitmap): Bitmap {
        var b = src
        while (b.width > 60) {
            val next = Bitmap.createScaledBitmap(b, b.width / 2, b.height / 2, true)
            if (b !== src) b.recycle()
            b = next
        }
        // glass wants a livelier colour than the photo: saturate once here instead of on every frame of every pane
        val out = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(b, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.35f) }) })
        if (b !== src) b.recycle()
        return out
    }

    private const val TARGET_LUMA = 0.24
    private const val FRESH_ENOUGH = 6

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)
}
