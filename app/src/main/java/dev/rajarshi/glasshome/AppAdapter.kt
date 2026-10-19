package dev.rajarshi.glasshome

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

/** Banner (or icon fallback) loading off the main thread, cached by package. */
private object Art {
    class Result(val state: Drawable.ConstantState?, val fallback: Drawable, val banner: Boolean)

    private val cache = LruCache<String, Result>(64)
    private val pool = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    fun load(ctx: Context, item: AppItem, done: (Result) -> Unit) {
        cache.get(item.pkg)?.let { done(it); return }
        pool.execute {
            val pm = ctx.packageManager
            val info = pm.getActivityInfo(item.launch.component!!, 0)
            val banner = info.loadBanner(pm) ?: info.applicationInfo.loadBanner(pm)
            val drawable = banner ?: info.loadIcon(pm)
            val result = Result(drawable.constantState, drawable, banner != null)
            cache.put(item.pkg, result)
            main.post { done(result) }
        }
    }
}

/**
 * @param nameAbove true on the shelf, where the focused app's name is shown above it, so a
 *   tile with a banner needs no label of its own. The grid shows the label on focus instead.
 */
class AppAdapter(
    private val items: List<AppItem>,
    private val nameAbove: Boolean,
    private val onFocus: ((AppItem) -> Unit)? = null,
    private val onClick: (AppItem) -> Unit,
) : RecyclerView.Adapter<AppAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val art: ImageView = v.findViewById(R.id.art)
        val label: TextView = v.findViewById(R.id.label)
        var item: AppItem? = null
        var hasBanner = false
        val spring = FocusSpring(v, focusedScale = 1.1f, lift = 12f * v.resources.displayMetrics.density)
    }

    private fun VH.syncLabel(focused: Boolean) {
        label.visibility = when {
            !hasBanner -> View.VISIBLE
            focused && !nameAbove -> View.VISIBLE
            else -> View.INVISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v).also { vh ->
            v.setOnFocusChangeListener { _, focused ->
                vh.spring.onFocus(focused)
                vh.syncLabel(focused)
                if (focused) vh.item?.let { onFocus?.invoke(it) }
            }
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        h.item = item
        h.label.text = item.label
        h.itemView.setOnClickListener { onClick(item) }
        val pad = (28 * h.itemView.resources.displayMetrics.density).toInt()

        if (item.pkg == AppRepository.ALL_PKG) {
            h.art.setImageResource(R.drawable.ic_apps)
            h.art.setPadding(pad, pad / 2, pad, pad / 2)
            h.art.scaleType = ImageView.ScaleType.FIT_CENTER
            h.hasBanner = false
            h.syncLabel(h.itemView.hasFocus())
            return
        }

        h.art.setImageDrawable(null)
        h.hasBanner = false
        h.syncLabel(h.itemView.hasFocus())
        Art.load(h.itemView.context, item) { r ->
            if (h.item?.pkg != item.pkg) return@load
            h.art.setImageDrawable(r.state?.newDrawable() ?: r.fallback)
            h.hasBanner = r.banner
            val p = if (r.banner) 0 else pad
            h.art.setPadding(p, p, p, p)
            h.art.scaleType = if (r.banner) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
            h.syncLabel(h.itemView.hasFocus())
        }
    }
}
