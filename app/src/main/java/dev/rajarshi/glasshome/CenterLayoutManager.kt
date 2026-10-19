package dev.rajarshi.glasshome

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Keeps the focused tile centred once it passes the middle; RecyclerView clamps at both ends. */
class CenterLayoutManager(ctx: Context) : LinearLayoutManager(ctx, HORIZONTAL, false) {
    private val ease = DecelerateInterpolator(1.6f)

    /** Keep the neighbours attached, so D-pad focus search always finds the next tile instead of leaving the row. */
    override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
        extraLayoutSpace[0] = width / 2
        extraLayoutSpace[1] = width / 2
    }

    override fun requestChildRectangleOnScreen(
        parent: RecyclerView, child: View, rect: Rect, immediate: Boolean, focusedChildVisible: Boolean,
    ): Boolean {
        val dx = (child.left + child.width / 2) -
            (parent.paddingLeft + (parent.width - parent.paddingLeft - parent.paddingRight) / 2)
        if (dx == 0) return false
        if (immediate) parent.scrollBy(dx, 0) else parent.smoothScrollBy(dx, 0, ease, 260)
        return true
    }
}
