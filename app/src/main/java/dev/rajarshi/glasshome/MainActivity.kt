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
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var home: ViewGroup
    private lateinit var sheet: View
    private lateinit var sheetGlass: View
    private lateinit var rvDock: RecyclerView
    private lateinit var rvGrid: RecyclerView
    private lateinit var appName: TextView
    private lateinit var greeting: TextView

    private var sheetOpen = false
    private var sheetSpring: SpringAnimation? = null
    private val handler = Handler(Looper.getMainLooper())
    private val rotate = object : Runnable {
        override fun run() {
            Wallpapers.next(animate = true)
            updateGreeting()
            handler.postDelayed(this, ROTATE_MS)
        }
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

        home = findViewById(R.id.home)
        sheet = findViewById(R.id.sheet)
        sheetGlass = findViewById(R.id.sheetGlass)
        rvDock = findViewById(R.id.rvDock)
        rvGrid = findViewById(R.id.rvGrid)
        appName = findViewById(R.id.appName)
        greeting = findViewById(R.id.greeting)

        rvDock.layoutManager = CenterLayoutManager(this)
        rvGrid.layoutManager = GridLayoutManager(this, 5)
        rvDock.itemAnimator = null
        rvGrid.itemAnimator = null

        findViewById<View>(R.id.btnWifi).bind { open(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        findViewById<View>(R.id.btnSettings).apply {
            bind { open(Intent(Settings.ACTION_SETTINGS)) }
            // Long-press toggles effects: opaque panels and no motion. Auto-on if system animations are off.
            setOnLongClickListener {
                Fx.reduced = !Fx.reduced
                prefs.edit().putBoolean("reduced", Fx.reduced).apply()
                Toast.makeText(
                    this@MainActivity,
                    if (Fx.reduced) R.string.effects_reduced else R.string.effects_full,
                    Toast.LENGTH_SHORT,
                ).show()
                window.decorView.invalidate()
                true
            }
        }

        Wallpapers.init(this)
        Wallpapers.next(animate = false)
        updateGreeting()
        reload()
    }

    private fun View.bind(action: () -> Unit) {
        val spring = FocusSpring(this, focusedScale = 1.06f, lift = 0f)
        setOnFocusChangeListener { _, focused -> spring.onFocus(focused) }
        setOnClickListener { action() }
    }

    private fun updateGreeting() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        greeting.setText(
            when {
                h < 12 -> R.string.good_morning
                h < 17 -> R.string.good_afternoon
                else -> R.string.good_evening
            },
        )
    }

    private fun reload() {
        val lists = AppRepository.load(this)
        val allTile = AppItem(getString(R.string.all_apps), AppRepository.ALL_PKG, Intent())
        val shelf = lists.favorites.ifEmpty { lists.all.take(5) } + allTile

        rvDock.adapter = AppAdapter(shelf, nameAbove = true, onFocus = ::showName, onClick = { app ->
            if (app.pkg == AppRepository.ALL_PKG) openSheet() else open(app.launch)
        })
        rvGrid.adapter = AppAdapter(lists.all, nameAbove = false, onClick = { open(it.launch) })
        focusShelf(0)
    }

    private fun showName(app: AppItem) {
        if (appName.text == app.label) return
        appName.text = app.label
        if (!Fx.reduced) {
            appName.alpha = 0.2f
            appName.animate().alpha(1f).setDuration(160).start()
        }
    }

    private fun focusShelf(position: Int) {
        rvDock.post {
            rvDock.scrollToPosition(position)
            rvDock.post { rvDock.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }
        }
    }

    // --- All Apps sheet ---------------------------------------------------------------------

    private fun openSheet() {
        if (sheetOpen) return
        sheetOpen = true
        home.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        sheet.visibility = View.VISIBLE
        slideSheetTo(0f, from = resources.displayMetrics.heightPixels.toFloat())
        rvGrid.post { rvGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
    }

    private fun closeSheet() {
        if (!sheetOpen) return
        sheetOpen = false
        home.visibility = View.VISIBLE
        home.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        slideSheetTo(resources.displayMetrics.heightPixels.toFloat(), from = sheet.translationY)
        focusShelf((rvDock.adapter?.itemCount ?: 1) - 1)
    }

    /** Spring from the live position, so grabbing it mid-flight just re-targets. Damping 1.0 = no bounce. */
    private fun slideSheetTo(target: Float, from: Float) {
        sheetSpring?.cancel()
        if (Fx.reduced) {
            sheet.translationY = target
            sheet.visibility = if (sheetOpen) View.VISIBLE else View.GONE
            if (sheetOpen) home.visibility = View.INVISIBLE
            sheetGlass.invalidate()
            return
        }
        sheet.translationY = from
        sheetSpring = SpringAnimation(sheet, DynamicAnimation.TRANSLATION_Y, target).apply {
            spring = SpringForce(target).setDampingRatio(1f).setStiffness(320f)
            // Glass samples the wallpaper by screen position, so repaint it while it moves.
            addUpdateListener { _, _, _ -> sheetGlass.invalidate() }
            addEndListener { _, _, _, _ ->
                if (sheetOpen) home.visibility = View.INVISIBLE else sheet.visibility = View.GONE
                sheetGlass.invalidate()
            }
            start()
        }
    }

    // --- plumbing ----------------------------------------------------------------------------

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
        updateGreeting()
        handler.postDelayed(rotate, ROTATE_MS)
    }

    override fun onStop() {
        handler.removeCallbacks(rotate)
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    /** Home pressed while we are already showing: close the sheet and go back to the first tile. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (sheetOpen) closeSheet()
            focusShelf(0)
        }
    }

    // Back closes the sheet; a launcher has nowhere else to go back to.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { closeSheet() }

    private companion object {
        const val ROTATE_MS = 10 * 60 * 1000L
    }
}
