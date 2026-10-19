package dev.raj.lucerra

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

data class AppItem(val label: String, val pkg: String, val launch: Intent)

/** [pinned] is what Home shows (max [AppRepository.MAX_PINS], in pin order); [others] is everything else, A to Z. */
class AppLists(val pinned: List<AppItem>, val others: List<AppItem>)

enum class PinResult { PINNED, UNPINNED, FULL }

object AppRepository {
    const val MAX_PINS = 5
    private const val PINS_KEY = "pins"

    fun load(ctx: Context): AppLists {
        val pm = ctx.packageManager
        val hidden = ctx.resources.getStringArray(R.array.hidden_packages).toSet() + ctx.packageName

        fun query(category: String): List<ResolveInfo> =
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)

        // Leanback activities first so TV-ready entry points win over phone launchers.
        val all = (query(Intent.CATEGORY_LEANBACK_LAUNCHER) + query(Intent.CATEGORY_LAUNCHER))
            .filter { it.activityInfo.packageName !in hidden }
            .distinctBy { it.activityInfo.packageName }
            .map {
                val ai = it.activityInfo
                AppItem(
                    label = it.loadLabel(pm).toString(),
                    pkg = ai.packageName,
                    launch = Intent(Intent.ACTION_MAIN)
                        .setClassName(ai.packageName, ai.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
                )
            }
            .sortedBy { it.label.lowercase() }

        val pinned = savedPins(ctx).mapNotNull { pkg -> all.find { it.pkg == pkg } }.take(MAX_PINS)
        return AppLists(pinned, all.filter { it !in pinned })
    }

    /** Pin or unpin [pkg]; a sixth pin is refused. */
    fun togglePin(ctx: Context, pkg: String): PinResult {
        val current = load(ctx).pinned.map { it.pkg }
        val (next, result) = when {
            pkg in current -> (current - pkg) to PinResult.UNPINNED
            current.size >= MAX_PINS -> return PinResult.FULL
            else -> (current + pkg) to PinResult.PINNED
        }
        prefs(ctx).edit().putString(PINS_KEY, next.joinToString(",")).apply()
        return result
    }

    /** Swap [pkg] with its neighbour one step left (-1) or right (+1); no-op at the ends. */
    fun movePin(ctx: Context, pkg: String, delta: Int) {
        val order = load(ctx).pinned.map { it.pkg }.toMutableList()
        val i = order.indexOf(pkg)
        val j = i + delta
        if (i < 0 || j !in order.indices) return
        order[i] = order[j].also { order[j] = order[i] }
        prefs(ctx).edit().putString(PINS_KEY, order.joinToString(",")).apply()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    /** Saved order, or the config favourites until the user pins something. */
    private fun savedPins(ctx: Context): List<String> =
        prefs(ctx).getString(PINS_KEY, null)?.split(",")?.filter { it.isNotEmpty() }
            ?: ctx.resources.getStringArray(R.array.favorite_packages).toList()
}
