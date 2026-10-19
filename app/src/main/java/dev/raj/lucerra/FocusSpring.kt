package dev.raj.lucerra

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * Focus lift driven by springs. Springs re-target from the live value and velocity, so a fast
 * D-pad run never waits for the previous animation (interruptible). Damping 1.0 = no overshoot.
 * Stiffness 400 is about a 0.3 s response.
 */
class FocusSpring(private val view: View, private val focusedScale: Float, private val lift: Float) {
    private val sx = spring(DynamicAnimation.SCALE_X, 1f)
    private val sy = spring(DynamicAnimation.SCALE_Y, 1f)
    private val sz = spring(DynamicAnimation.TRANSLATION_Z, 0f)

    private fun spring(prop: DynamicAnimation.ViewProperty, rest: Float) =
        SpringAnimation(view, prop, rest).apply {
            spring = SpringForce(rest).setDampingRatio(1f).setStiffness(400f)
        }

    fun onFocus(focused: Boolean) {
        if (Fx.reduced) {
            sx.cancel(); sy.cancel(); sz.cancel()
            view.scaleX = 1f; view.scaleY = 1f; view.translationZ = 0f
            return
        }
        val scale = if (focused) focusedScale else 1f
        sx.animateToFinalPosition(scale)
        sy.animateToFinalPosition(scale)
        sz.animateToFinalPosition(if (focused) lift else 0f)
    }
}
