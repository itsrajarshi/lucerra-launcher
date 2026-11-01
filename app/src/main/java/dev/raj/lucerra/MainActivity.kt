package dev.raj.lucerra

import android.app.Activity
import android.app.ActivityOptions
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var home: ViewGroup
    private lateinit var sheet: View
    private lateinit var sheetGlass: View
    private lateinit var rvDock: RecyclerView
    private lateinit var rvPinned: RecyclerView
    private lateinit var rvGrid: RecyclerView
    private lateinit var appName: TextView
    private lateinit var greeting: TextView
    private lateinit var onboarding: Onboarding

    private var pinnedItems = emptyList<AppItem>()
    private var otherItems = emptyList<AppItem>()
    private var sheetOpen = false
    private var sheetSpring: SpringAnimation? = null
    private var enterPending = false
    private val bottom by lazy { findViewById<View>(R.id.bottom) }
    private val handler = Handler(Looper.getMainLooper())
    private val rotate = object : Runnable {
        override fun run() {
            Wallpapers.next(animate = true)
            updateGreeting()
            handler.postDelayed(this, ROTATE_MS)
        }
    }
    private val nowPlaying by lazy { findViewById<View>(R.id.nowPlaying) }
    private val np by lazy { NowPlaying(this) { showNowPlaying(it) } }
    private val hideNowPlaying = Runnable { fadeNowPlaying(false) }
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

        // Effects reduce automatically when the system animations are off (opaque panels, no motion).
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val animationsOff = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        Fx.reduced = prefs.getBoolean("reduced", animationsOff)

        home = findViewById(R.id.home)
        sheet = findViewById(R.id.sheet)
        sheetGlass = findViewById(R.id.sheetGlass)
        (sheetGlass as GlassFrame).slidingParent = sheet
        rvDock = findViewById(R.id.rvDock)
        rvPinned = findViewById(R.id.rvPinned)
        rvGrid = findViewById(R.id.rvGrid)
        appName = findViewById(R.id.appName)
        greeting = findViewById(R.id.greeting)

        rvDock.layoutManager = CenterLayoutManager(this)
        rvPinned.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvGrid.layoutManager = GridLayoutManager(this, 5)
        listOf(rvDock, rvPinned, rvGrid).forEach { it.itemAnimator = null }

        findViewById<View>(R.id.btnProfile).setOnClickListener { topMenu() }
        findViewById<View>(R.id.btnAssistant).setOnClickListener { launchAssistant() }
        findViewById<View>(R.id.npPrev).setOnClickListener { np.previous() }
        findViewById<View>(R.id.npPlay).setOnClickListener { np.toggle() }
        findViewById<View>(R.id.npNext).setOnClickListener { np.next() }
        findViewById<ImageView>(R.id.npArt).apply {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) = o.setRoundRect(0, 0, v.width, v.height, 7f * resources.displayMetrics.density)
            }
            clipToOutline = true
        }
        onboarding = Onboarding(this) { loadProfile(); updateGreeting(); lockHome(false); focusShelf(0) }
        loadProfile()
        Wallpapers.init(this)
        // Rebuilt after the system freed us behind a heavy app: keep the wallpaper, no hard cut.
        if (Wallpapers.cur == null) Wallpapers.next(animate = false)
        updateGreeting()
        reload()
        if (intent.getBooleanExtra(EXTRA_ALL_APPS, false)) openSheet()
        if (!Profile.done(this)) beginOnboarding(tour = false)
    }

    private fun beginOnboarding(tour: Boolean) {
        if (sheetOpen) closeSheet()
        lockHome(true)
        onboarding.start(tour)
    }

    /** While the welcome flow is up, Home must not take focus or key presses. */
    private fun lockHome(on: Boolean) {
        home.descendantFocusability = if (on) ViewGroup.FOCUS_BLOCK_DESCENDANTS else ViewGroup.FOCUS_BEFORE_DESCENDANTS
    }

    /** The saved name and colour; assets/profile.jpg (square) still wins as a photo if one was shipped and no profile was set. */
    private fun loadProfile() {
        val name = Profile.name(this)
        findViewById<TextView>(R.id.avatarInitial).apply {
            text = name.take(1).uppercase().ifEmpty { getString(R.string.profile_initial) }
            if (Profile.done(this@MainActivity)) {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Profile.COLORS[Profile.colorIndex(this@MainActivity)])
                }
            }
        }
        if (Profile.done(this)) { findViewById<ImageView>(R.id.avatar).visibility = View.GONE; return }
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assets.open("profile.jpg").use { BitmapFactory.decodeStream(it, null, bounds) }
            val opts = BitmapFactory.Options().apply { inSampleSize = maxOf(1, bounds.outWidth / 128) }
            val bmp = assets.open("profile.jpg").use { BitmapFactory.decodeStream(it, null, opts) } ?: return
            findViewById<ImageView>(R.id.avatar).apply {
                setImageBitmap(bmp)
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, o: Outline) = o.setOval(0, 0, v.width, v.height)
                }
                clipToOutline = true
                visibility = View.VISIBLE
            }
        } catch (e: java.io.IOException) {
            // no photo shipped: the initial circle stays
        }
    }

    /** Up from the shelf opens All Apps. */
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (onboarding.showing && e.keyCode == KeyEvent.KEYCODE_ALL_APPS) return true
        if (e.action == KeyEvent.ACTION_DOWN && e.keyCode == KeyEvent.KEYCODE_DPAD_UP && !sheetOpen) {
            val focus = currentFocus
            if (focus != null && rvDock.findContainingItemView(focus) != null) { openSheet(); return true }
        }
        // The remote's grid button (left of the home button) opens All Apps from anywhere.
        if (e.action == KeyEvent.ACTION_DOWN && e.keyCode == KeyEvent.KEYCODE_ALL_APPS) { openSheet(); return true }
        return super.dispatchKeyEvent(e)
    }

    private fun launchAssistant() = open(Intent(Intent.ACTION_ASSIST))

    /** The "press up" hint fades in when Home shows, stays a few seconds, then fades away for good. */
    private fun startHint() {
        val hint = findViewById<View>(R.id.hint)
        hint.animate().cancel()
        if (sheetOpen) { hint.alpha = 0f; return }
        if (Fx.reduced) { hint.alpha = 1f; return }
        hint.alpha = 0f
        hint.animate().alpha(1f).setStartDelay(500).setDuration(700)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                hint.animate().alpha(0f).setStartDelay(HINT_HOLD_MS).setDuration(900)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }
            .start()
    }

    private fun stopHint() = findViewById<View>(R.id.hint).animate().cancel()

    private fun updateGreeting() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val hello = getString(
            when {
                h < 12 -> R.string.good_morning
                h < 17 -> R.string.good_afternoon
                else -> R.string.good_evening
            },
        )
        val name = Profile.name(this)
        greeting.text = if (name.isEmpty()) hello else "$hello, ${name.uppercase()}"
    }

    private fun reload() {
        val lists = AppRepository.load(this)
        pinnedItems = lists.pinned
        otherItems = lists.others

        rvDock.adapter = AppAdapter(pinnedItems, nameAbove = true, onFocus = ::showName, onLongClick = ::pinMenu, onClick = { open(it.launch) })
        rvPinned.adapter = AppAdapter(pinnedItems, nameAbove = false, onLongClick = ::pinMenu, onClick = { open(it.launch) })
        rvGrid.adapter = AppAdapter(otherItems, nameAbove = false, onLongClick = ::gridMenu, onClick = { open(it.launch) })
        if (pinnedItems.isEmpty()) appName.text = ""
        if (!sheetOpen) focusShelf(0)
    }

    /** Hold OK on any tile in All Apps. */
    private fun togglePin(app: AppItem) {
        val msg = when (AppRepository.togglePin(this, app.pkg)) {
            PinResult.PINNED -> R.string.pinned
            PinResult.UNPINNED -> R.string.unpinned
            PinResult.FULL -> R.string.pins_full
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        reload()
        focusApp(app.pkg)
    }

    /** One small glass dialog with up to three rows; rows are (label, action). */
    private fun showMenu(title: String, rows: List<Pair<String, () -> Unit>>, topRight: Boolean = false) {
        val d = Dialog(this)
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (topRight) {
                setGravity(Gravity.TOP or Gravity.END)
                val dp = resources.displayMetrics.density
                attributes = attributes.apply { x = (48 * dp).toInt(); y = (84 * dp).toInt() }
            }
        }
        d.setContentView(R.layout.dialog_pin_menu)
        d.findViewById<TextView>(R.id.menuTitle).text = title
        listOf(R.id.menuLeft, R.id.menuRight, R.id.menuRemove, R.id.menuExtra, R.id.menuExtra2, R.id.menuExtra3).forEachIndexed { n, id ->
            val v = d.findViewById<TextView>(id)
            val row = rows.getOrNull(n)
            if (row == null) { v.visibility = View.GONE; return@forEachIndexed }
            v.visibility = View.VISIBLE
            v.text = row.first
            v.setOnClickListener { d.dismiss(); row.second() }
        }
        d.show()
        d.findViewById<View>(R.id.menuLeft).takeIf { rows.isNotEmpty() }?.requestFocus()
        popIn(d.findViewById<View>(R.id.menuTitle).parent as View, topRight)
    }

    /** The menu grows out of its corner on a spring; no bounce, so it settles rather than wobbles. */
    private fun popIn(v: View, topRight: Boolean) {
        if (Fx.reduced) return
        v.post {
            v.pivotX = if (topRight) v.width.toFloat() else v.width / 2f
            v.pivotY = if (topRight) 0f else v.height / 2f
            v.scaleX = 0.9f; v.scaleY = 0.9f; v.alpha = 0f
            for (prop in listOf(DynamicAnimation.SCALE_X, DynamicAnimation.SCALE_Y, DynamicAnimation.ALPHA)) {
                SpringAnimation(v, prop, 1f).apply { spring = SpringForce(1f).setDampingRatio(1f).setStiffness(500f) }.start()
            }
        }
    }

    /** Hold OK on a favourite: move it along the shelf, or take it off Home. */
    private fun pinMenu(app: AppItem) {
        val i = pinnedItems.indexOfFirst { it.pkg == app.pkg }
        fun act(f: () -> Unit) = { f(); reload(); focusApp(app.pkg) }
        val rows = buildList {
            if (i > 0) add(getString(R.string.move_left) to act { AppRepository.movePin(this@MainActivity, app.pkg, -1) })
            if (i < pinnedItems.size - 1) add(getString(R.string.move_right) to act { AppRepository.movePin(this@MainActivity, app.pkg, 1) })
            add(getString(R.string.remove_from_home) to act { AppRepository.togglePin(this@MainActivity, app.pkg) })
        }
        showMenu(app.label, rows + infoRow(app) + uninstallRow(app))
    }

    /** Hold OK on an app that is not a favourite: add it to Home, or uninstall it. */
    private fun gridMenu(app: AppItem) = showMenu(
        app.label,
        listOf(getString(R.string.add_to_home) to { togglePin(app) }, infoRow(app), uninstallRow(app)),
    )

    private fun infoRow(app: AppItem) =
        getString(R.string.app_info) to { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + app.pkg))) }

    private fun uninstallRow(app: AppItem) =
        getString(R.string.uninstall) to { open(Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:" + app.pkg))) }

    /** The clock pill: Wi-Fi and Settings. */
    private fun topMenu() = showMenu(
        getString(R.string.quick_settings),
        listOf(
            getString(R.string.wifi) to { open(Intent(Settings.ACTION_WIFI_SETTINGS)) },
            getString(R.string.settings) to { open(Intent(Settings.ACTION_SETTINGS)) },
            getString(R.string.power) to {
                val svc = KeyService.instance
                if (svc != null) svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
                else Toast.makeText(this, R.string.power_needs_service, Toast.LENGTH_SHORT).show()
            },
            getString(if (WallpaperFetcher.enabled(this)) R.string.wp_on else R.string.wp_off) to {
                val on = !WallpaperFetcher.enabled(this)
                WallpaperFetcher.setEnabled(this, on)
                Toast.makeText(this, if (on) R.string.wp_on_msg else R.string.wp_off_msg, Toast.LENGTH_SHORT).show()
                if (on) WallpaperFetcher.refreshIfDue(this)
            },
            getString(R.string.edit_profile) to { beginOnboarding(tour = false) },
            getString(R.string.replay_tour) to { beginOnboarding(tour = true) },
        ),
        topRight = true,
    )

    private fun focusApp(pkg: String) {
        val pi = pinnedItems.indexOfFirst { it.pkg == pkg }
        if (!sheetOpen) { if (pi >= 0) focusShelf(pi) else focusShelf(0); return }
        val rv = if (pi >= 0) rvPinned else rvGrid
        val pos = if (pi >= 0) pi else otherItems.indexOfFirst { it.pkg == pkg }
        if (pos < 0) return
        rv.post {
            rv.scrollToPosition(pos)
            rv.post { rv.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus() }
        }
    }

    private fun showName(app: AppItem) {
        if (appName.text == app.label) return
        appName.text = app.label
        if (!Fx.reduced) {
            appName.alpha = 0.2f
            appName.translationY = 6f * resources.displayMetrics.density
            appName.animate().alpha(1f).translationY(0f).setDuration(220)
                .setInterpolator(DecelerateInterpolator(1.6f)).start()
        }
    }

    private fun focusShelf(position: Int) {
        rvDock.post {
            rvDock.scrollToPosition(position)
            rvDock.post { rvDock.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }
        }
    }

    // --- All Apps screen --------------------------------------------------------------------

    private fun openSheet() {
        if (sheetOpen) return
        sheetOpen = true
        stopHint()
        home.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        sheet.visibility = View.VISIBLE
        slideSheetTo(0f, from = resources.displayMetrics.heightPixels.toFloat())
        val rv = if (pinnedItems.isNotEmpty()) rvPinned else rvGrid
        rv.post { rv.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
    }

    private fun closeSheet() {
        if (!sheetOpen) return
        sheetOpen = false
        startHint()
        home.visibility = View.VISIBLE
        home.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        slideSheetTo(resources.displayMetrics.heightPixels.toFloat(), from = sheet.translationY)
        focusShelf(0)
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
            // The glass rides with the sheet (see GlassFrame.slidingParent), so sliding it never repaints the glass.
            addEndListener { _, _, _, _ ->
                if (sheetOpen) home.visibility = View.INVISIBLE else sheet.visibility = View.GONE
                sheetGlass.invalidate()
            }
            start()
        }
    }

    // --- plumbing ----------------------------------------------------------------------------

    /** Apps grow out of the tile that launched them (system-driven, so it costs us no frames). */
    private fun open(intent: Intent) {
        try {
            val src = currentFocus
            val opts = if (src != null && !Fx.reduced) {
                ActivityOptions.makeScaleUpAnimation(src, 0, 0, src.width, src.height).toBundle()
            } else null
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), opts)
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open that", Toast.LENGTH_SHORT).show()
        }
    }

    /** Coming back to Home (Home key or Back from an app): hide the content now, reveal it once the window is up. */
    private fun prepareEnter() {
        if (Fx.reduced || sheetOpen) return
        home.animate().cancel(); bottom.animate().cancel()
        home.alpha = 0f
        bottom.translationY = 24f * resources.displayMetrics.density
        enterPending = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || !enterPending) return
        enterPending = false
        home.animate().alpha(1f).setDuration(320).setInterpolator(DecelerateInterpolator(1.4f)).start()
        bottom.animate().translationY(0f).setDuration(380).setInterpolator(DecelerateInterpolator(1.8f)).start()
    }

    /** Show the widget while something plays; after a pause keep it a while so play can be pressed again. */
    private fun showNowPlaying(s: NowPlaying.State?) {
        handler.removeCallbacks(hideNowPlaying)
        if (s == null) { fadeNowPlaying(false); return }
        findViewById<TextView>(R.id.npTitle).text = s.title
        findViewById<TextView>(R.id.npArtist).text = s.artist
        findViewById<ImageView>(R.id.npArt).apply {
            if (s.art != null) { setPadding(0, 0, 0, 0); setImageBitmap(s.art) }
            else { val p = (6 * resources.displayMetrics.density).toInt(); setPadding(p, p, p, p); setImageResource(R.drawable.ic_note) }
        }
        findViewById<ImageView>(R.id.npPlay).setImageResource(if (s.playing) R.drawable.ic_pause else R.drawable.ic_play)
        fadeNowPlaying(true)
        if (!s.playing) handler.postDelayed(hideNowPlaying, NP_PAUSED_MS)
    }

    private fun fadeNowPlaying(show: Boolean) {
        val v = nowPlaying
        v.animate().cancel()
        if (show) {
            if (v.visibility != View.VISIBLE) { v.alpha = 0f; v.visibility = View.VISIBLE }
            if (Fx.reduced) v.alpha = 1f else v.animate().alpha(1f).setDuration(220).start()
        } else if (v.visibility == View.VISIBLE) {
            if (Fx.reduced) v.visibility = View.GONE
            else v.animate().alpha(0f).setDuration(220).withEndAction { v.visibility = View.GONE }.start()
        }
    }

    override fun onStart() {
        super.onStart()
        np.start()
        if (everStarted) prepareEnter()
        everStarted = true
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, f)
        updateGreeting()
        startHint()
        WallpaperFetcher.refreshIfDue(this)
        handler.postDelayed(rotate, ROTATE_MS)
    }

    override fun onStop() {
        np.stop()
        handler.removeCallbacks(hideNowPlaying)
        if (enterPending) { enterPending = false; home.alpha = 1f; bottom.translationY = 0f }
        stopHint()
        handler.removeCallbacks(rotate)
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    /** Home pressed while we are already showing: close All Apps and go back to the first tile. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ALL_APPS, false)) { openSheet(); return }
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (sheetOpen) closeSheet()
            focusShelf(0)
        }
    }

    // Back closes All Apps; a launcher has nowhere else to go back to.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { if (onboarding.showing) onboarding.onBack() else closeSheet() }

    companion object {
        const val EXTRA_ALL_APPS = "open_all_apps"
        private const val HINT_HOLD_MS = 5000L
        private const val NP_PAUSED_MS = 20_000L
        /** Static: Home is often destroyed behind a heavy app, and the new instance must still animate in. */
        private var everStarted = false
        private const val ROTATE_MS = 10 * 60 * 1000L
    }
}
