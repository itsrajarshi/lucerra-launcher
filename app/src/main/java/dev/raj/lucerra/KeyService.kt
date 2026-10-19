package dev.raj.lucerra

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Only job: the remote's grid button (KEYCODE_ALL_APPS) is claimed by the system and sent to TV Settings.
 * Key filtering sees it first, so we take it and open Glass Home's All Apps instead. Reads nothing else.
 */
class KeyService : AccessibilityService() {
    override fun onKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode != KeyEvent.KEYCODE_ALL_APPS) return false
        if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_ALL_APPS, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
        return true
    }

    override fun onServiceConnected() { instance = this }
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }

    companion object {
        var instance: KeyService? = null
            private set
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}
