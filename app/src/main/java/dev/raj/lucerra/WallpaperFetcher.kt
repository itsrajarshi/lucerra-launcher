package dev.raj.lucerra

import android.content.Context
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Once a day, pulls a few fresh landscape photos from the NASA Image and Video Library (images.nasa.gov,
 * no key, no account) into internal storage and keeps the newest [KEEP]. Only the image requests leave the
 * TV; nothing about the user or the box is sent. [Wallpapers] grades each photo dark when it decodes it.
 */
object WallpaperFetcher {
    private const val KEEP = 20
    private const val PER_RUN = 6
    private const val MAX_BYTES = 14_000_000
    private const val DAY_MS = 20L * 60 * 60 * 1000
    private val QUERIES = listOf("earth at night", "city lights", "aurora", "milky way", "galaxy", "nebula", "lightning storm earth")
    private val running = AtomicBoolean(false)

    private fun prefs(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    fun enabled(c: Context) = prefs(c).getBoolean("wp_auto", true)
    fun setEnabled(c: Context, on: Boolean) = prefs(c).edit().putBoolean("wp_auto", on).apply()
    fun dir(c: Context) = File(c.applicationContext.filesDir, "wallpapers").apply { mkdirs() }

    /** Cheap to call on every start: does nothing unless it is on and a day has passed. */
    fun refreshIfDue(ctx: Context) {
        val app = ctx.applicationContext
        if (!enabled(app) || System.currentTimeMillis() - prefs(app).getLong("wp_last", 0) < DAY_MS) return
        if (!running.compareAndSet(false, true)) return
        thread(name = "wallpaper-fetch") {
            try {
                val n = fetch(app)
                android.util.Log.i("Lucerra", "wallpaper fetch: $n new")
                if (n > 0) android.os.Handler(android.os.Looper.getMainLooper()).post { Wallpapers.init(app, showNewNow = true) }
            } catch (e: Exception) {
                // offline or the service is down: try again at the next start
                android.util.Log.i("Lucerra", "wallpaper fetch failed: $e")
            } finally {
                running.set(false)
            }
        }
    }

    private fun fetch(app: Context): Int {
        val dir = dir(app)
        val have = dir.list()?.toSet().orEmpty()
        val year = Calendar.getInstance().get(Calendar.YEAR) - 2
        val q = URLEncoder.encode(QUERIES.random(), "UTF-8")
        val search = get("https://images-api.nasa.gov/search?q=$q&media_type=image&year_start=$year&page_size=40")
        val items = JSONObject(String(search)).getJSONObject("collection").getJSONArray("items")
        val ids = (0 until items.length()).map { items.getJSONObject(it).getJSONArray("data").getJSONObject(0).getString("nasa_id") }
            .filter { "$it.jpg" !in have }.shuffled()
        var got = 0
        for (id in ids) {
            if (got >= PER_RUN) break
            if (grab(id, File(dir, "$id.jpg"))) got++
        }
        if (got > 0) {
            prefs(app).edit().putLong("wp_last", System.currentTimeMillis()).apply()
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(KEEP)?.forEach { it.delete() }
        }
        return got
    }

    /** Downloads the original if it is a landscape photo of at least 1920px and not huge; otherwise leaves nothing behind. */
    private fun grab(id: String, out: File): Boolean {
        val tmp = File(out.path + ".part")
        try {
            val assets = JSONObject(String(get("https://images-api.nasa.gov/asset/$id"))).getJSONObject("collection").getJSONArray("items")
            val url = (0 until assets.length()).map { assets.getJSONObject(it).getString("href").replace("http://", "https://") }
                .firstOrNull { it.endsWith("~orig.jpg") } ?: return false
            val c = open(url)
            if (c.contentLength !in 1..MAX_BYTES) { c.disconnect(); return false }
            c.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tmp.path, b)
            if (b.outWidth < 1920 || b.outWidth.toFloat() / b.outHeight < 1.3f) return false
            return tmp.renameTo(out)
        } catch (e: Exception) {
            return false
        } finally {
            tmp.delete()
        }
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000; readTimeout = 30_000
        setRequestProperty("User-Agent", "LucerraLauncher")
    }

    private fun get(url: String): ByteArray = open(url).run { try { inputStream.use { it.readBytes() } } finally { disconnect() } }
}
