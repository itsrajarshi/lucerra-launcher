package dev.rajarshi.glasshome

import android.app.Activity
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
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
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

    private var pinnedItems = emptyList<AppItem>()
    private var otherItems = emptyList<AppItem>()
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

        // Effects reduce automatically when the system animations are off (opaque panels, no motion).
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val animationsOff = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        Fx.reduced = prefs.getBoolean("reduced", animationsOff)

        home = findViewById(R.id.home)
        sheet = findViewById(R.id.sheet)
        sheetGlass = findViewById(R.id.sheetGlass)
        rvDock = findViewById(R.id.rvDock)
        rvPinned = findViewById(R.id.rvPinned)
        rvGrid = findViewById(R.id.rvGrid)
        appName = findViewById(R.id.appName)
        greeting = findViewById(R.id.greeting)

        rvDock.layoutManager = CenterLayoutManager(this)
        rvPinned.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvGrid.layoutManager = GridLayoutManager(this, 5)
        listOf(rvDock, rvPinned, rvGrid).forEach { it.itemAnimator = null }

        findViewById<View>(R.id.btnAssistant).setOnClickListener { launchAssistant() }
        loadProfile()
        Wallpapers.init(this)
        Wallpapers.next(animate = false)
        updateGreeting()
        reload()
    }

    /** assets/profile.jpg (square, any size) if present, else the configured initial. */
    private fun loadProfile() {
        findViewById<TextView>(R.id.avatarInitial).text = getString(R.string.profile_initial)
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
        if (e.action == KeyEvent.ACTION_DOWN && e.keyCode == KeyEvent.KEYCODE_DPAD_UP && !sheetOpen) {
            val focus = currentFocus
            if (focus != null && rvDock.findContainingItemView(focus) != null) { openSheet(); return true }
        }
        // The remote's grid button (left of the home button) opens All Apps from anywhere.
        if (e.action == KeyEvent.ACTION_DOWN && e.keyCode == KeyEvent.KEYCODE_ALL_APPS) { openSheet(); return true }
        return super.dispatchKeyEvent(e)
    }

    private fun launchAssistant() = open(Intent(Intent.ACTION_ASSIST))

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
        pinnedItems = lists.pinned
        otherItems = lists.others

        rvDock.adapter = AppAdapter(pinnedItems, nameAbove = true, onFocus = ::showName, onLongClick = ::pinMenu, onClick = { open(it.launch) })
        rvPinned.adapter = AppAdapter(pinnedItems, nameAbove = false, onLongClick = ::pinMenu, onClick = { open(it.launch) })
        rvGrid.adapter = AppAdapter(otherItems, nameAbove = false, onLongClick = ::togglePin, onClick = { open(it.launch) })
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

    /** Hold OK on a favourite: move it along the shelf, or take it off Home. */
    private fun pinMenu(app: AppItem) {
        val i = pinnedItems.indexOfFirst { it.pkg == app.pkg }
        val d = Dialog(this)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.setContentView(R.layout.dialog_pin_menu)
        d.findViewById<TextView>(R.id.menuTitle).text = app.label
        fun row(id: Int, enabled: Boolean, act: () -> Unit) = d.findViewById<View>(id).apply {
            if (!enabled) { visibility = View.GONE; return@apply }
            setOnClickListener { d.dismiss(); act(); reload(); focusApp(app.pkg) }
        }
        row(R.id.menuLeft, i > 0) { AppRepository.movePin(this, app.pkg, -1) }
        row(R.id.menuRight, i < pinnedItems.size - 1) { AppRepository.movePin(this, app.pkg, 1) }
        row(R.id.menuRemove, true) { AppRepository.togglePin(this, app.pkg) }
        d.show()
        d.findViewById<View>(if (i > 0) R.id.menuLeft else if (i < pinnedItems.size - 1) R.id.menuRight else R.id.menuRemove).requestFocus()
    }

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
        home.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        sheet.visibility = View.VISIBLE
        slideSheetTo(0f, from = resources.displayMetrics.heightPixels.toFloat())
        val rv = if (pinnedItems.isNotEmpty()) rvPinned else rvGrid
        rv.post { rv.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
    }

    private fun closeSheet() {
        if (!sheetOpen) return
        sheetOpen = false
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

    /** Home pressed while we are already showing: close All Apps and go back to the first tile. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (sheetOpen) closeSheet()
            focusShelf(0)
        }
    }

    // Back closes All Apps; a launcher has nowhere else to go back to.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { closeSheet() }

    private companion object {
        const val ROTATE_MS = 10 * 60 * 1000L
    }
}
