package dev.rajarshi.glasshome

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class MainActivity : Activity() {
    private lateinit var rvFav: RecyclerView
    private lateinit var rvAll: RecyclerView
    private lateinit var favLabel: View
    private val handler = Handler(Looper.getMainLooper())
    private val rotate = object : Runnable {
        override fun run() { Wallpapers.next(animate = true); handler.postDelayed(this, ROTATE_MS) }
    }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = reload()
    }

    /** Lay out as if the screen were 960dp wide on every TV resolution (720p, 1080p, 4K). */
    override fun attachBaseContext(base: Context) {
        val cfg = Configuration(base.resources.configuration)
        cfg.densityDpi = base.resources.displayMetrics.widthPixels * 160 / 960
        super.attachBaseContext(base.createConfigurationContext(cfg))
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val animationsOff = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        Fx.reduced = prefs.getBoolean("reduced", animationsOff)

        rvFav = findViewById(R.id.rvFav)
        rvAll = findViewById(R.id.rvAll)
        favLabel = findViewById(R.id.favLabel)
        rvFav.layoutManager = CenterLayoutManager(this)
        rvAll.layoutManager = CenterLayoutManager(this)
        rvFav.itemAnimator = null
        rvAll.itemAnimator = null

        findViewById<View>(R.id.btnSettings).bind { open(Intent(Settings.ACTION_SETTINGS)) }
        findViewById<View>(R.id.btnWifi).bind { open(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        val effects = findViewById<TextView>(R.id.btnEffects)
        fun renderEffects() = effects.setText(if (Fx.reduced) R.string.effects_reduced else R.string.effects_full)
        renderEffects()
        effects.bind {
            Fx.reduced = !Fx.reduced
            prefs.edit().putBoolean("reduced", Fx.reduced).apply()
            renderEffects()
            window.decorView.invalidate()
        }

        Wallpapers.next(animate = false)
        reload()
    }

    private fun View.bind(action: () -> Unit) {
        val spring = FocusSpring(this, focusedScale = 1.06f, lift = 0f)
        setOnFocusChangeListener { _, focused -> spring.onFocus(focused) }
        setOnClickListener { action() }
    }

    private fun reload() {
        val lists = AppRepository.load(this)
        rvFav.adapter = AppAdapter(lists.favorites, ::launch)
        rvAll.adapter = AppAdapter(lists.all, ::launch)
        val showFav = lists.favorites.isNotEmpty()
        rvFav.visibility = if (showFav) View.VISIBLE else View.GONE
        favLabel.visibility = rvFav.visibility
        focusFirst()
    }

    private fun focusFirst() {
        val target = if (rvFav.visibility == View.VISIBLE) rvFav else rvAll
        target.post { target.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
    }

    private fun launch(app: AppItem) = open(app.launch)

    private fun open(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open that", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, f)
        handler.postDelayed(rotate, ROTATE_MS)
    }

    override fun onStop() {
        handler.removeCallbacks(rotate)
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    /** Home pressed while we are already showing: jump back to the start. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            rvFav.scrollToPosition(0)
            rvAll.scrollToPosition(0)
            focusFirst()
        }
    }

    // A launcher has nowhere to go back to.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {}

    private companion object {
        const val ROTATE_MS = 10 * 60 * 1000L
    }
}
