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

    private lateinit var bottom: View
    private lateinit var hint: View
    private lateinit var btnProfile: View
    private lateinit var prefs: android.content.SharedPreferences

    private var sheetOpen = false
    private var fullScreen = false
    private var shelfPos = 0
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

        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
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

        bottom = findViewById(R.id.bottom)
        hint = findViewById(R.id.hint)
        hint.visibility = if (prefs.getInt("fsUses", 0) < 3) View.VISIBLE else View.GONE
        loadProfile()

        btnProfile = findViewById<View>(R.id.btnProfile).apply {
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

    /** assets/profile.jpg (square, any size) if present, else the configured initial. */
    private fun loadProfile() {
        findViewById<TextView>(R.id.avatarInitial).text = getString(R.string.profile_initial)
        try {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assets.open("profile.jpg").use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = maxOf(1, bounds.outWidth / 128) }
            val bmp = assets.open("profile.jpg").use { android.graphics.BitmapFactory.decodeStream(it, null, opts) } ?: return
            findViewById<android.widget.ImageView>(R.id.avatar).apply {
                setImageBitmap(bmp)
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: View, o: android.graphics.Outline) = o.setOval(0, 0, v.width, v.height)
                }
                clipToOutline = true
                visibility = View.VISIBLE
            }
        } catch (e: java.io.IOException) {
            // no photo shipped: the initial circle stays
        }
    }

    // --- Full screen: wallpaper only; the shelf slides away ---------------------------------

    private fun enterFullScreen() {
        if (fullScreen) return
        fullScreen = true
        prefs.edit().putInt("fsUses", prefs.getInt("fsUses", 0) + 1).apply()
        shelfPos = rvDock.findContainingViewHolder(currentFocus ?: rvDock)?.adapterPosition?.takeIf { it >= 0 } ?: shelfPos
        (bottom as ViewGroup).descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        bottom.clearFocus()
        btnProfile.isFocusable = false
        val dy = bottom.height.toFloat()
        if (Fx.reduced) { bottom.alpha = 0f; bottom.visibility = View.INVISIBLE; return }
        bottom.animate().translationY(dy).alpha(0f).setDuration(320)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.6f))
            .withEndAction { if (fullScreen) bottom.visibility = View.INVISIBLE }.start()
    }

    private fun exitFullScreen() {
        if (!fullScreen) return
        fullScreen = false
        btnProfile.isFocusable = false
        bottom.visibility = View.VISIBLE
        (bottom as ViewGroup).descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        if (Fx.reduced) { bottom.alpha = 1f; bottom.translationY = 0f }
        else bottom.animate().translationY(0f).alpha(1f).setDuration(320)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.6f)).start()
        if (prefs.getInt("fsUses", 0) >= 3) hint.visibility = View.GONE
        focusShelf(shelfPos)
    }

    /** Up from the shelf enters full screen; Up again reaches the profile pill; Down or Back returns. */
    override fun dispatchKeyEvent(e: android.view.KeyEvent): Boolean {
        if (e.action == android.view.KeyEvent.ACTION_DOWN && !sheetOpen) {
            val focus = currentFocus
            when (e.keyCode) {
                android.view.KeyEvent.KEYCODE_DPAD_UP -> when {
                    !fullScreen && focus != null && rvDock.findContainingItemView(focus) != null -> { enterFullScreen(); return true }
                    fullScreen -> { btnProfile.isFocusable = true; btnProfile.requestFocus(); return true }
                }
                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> if (fullScreen) { exitFullScreen(); return true }
                android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER ->
                    if (fullScreen && focus !== btnProfile) { exitFullScreen(); return true }
            }
        }
        return super.dispatchKeyEvent(e)
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
            appName.translationY = 6f * resources.displayMetrics.density
            appName.animate().alpha(1f).translationY(0f).setDuration(220)
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.6f)).start()
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
            shelfPos = 0
            exitFullScreen()
            focusShelf(0)
        }
    }

    // Back closes the sheet; a launcher has nowhere else to go back to.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { if (fullScreen) exitFullScreen() else closeSheet() }

    private companion object {
        const val ROTATE_MS = 10 * 60 * 1000L
    }
}
