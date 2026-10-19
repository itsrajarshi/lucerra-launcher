package dev.rajarshi.glasshome

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

data class AppItem(val label: String, val pkg: String, val launch: Intent)

class AppLists(val favorites: List<AppItem>, val all: List<AppItem>)

object AppRepository {
    fun load(ctx: Context): AppLists {
        val pm = ctx.packageManager
        val hidden = ctx.resources.getStringArray(R.array.hidden_packages).toSet() + ctx.packageName
        val favOrder = ctx.resources.getStringArray(R.array.favorite_packages)

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

        val favorites = favOrder.mapNotNull { pkg -> all.find { it.pkg == pkg } }
        return AppLists(favorites, all)
    }
}
