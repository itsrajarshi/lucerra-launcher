package dev.raj.lucerra

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** The saved profile: a name and one of [COLORS] for the circle top right on Home. */
object Profile {
    val COLORS = intArrayOf(
        0xFF6C8CFF.toInt(), 0xFF8E7CFF.toInt(), 0xFFE56B9A.toInt(), 0xFFF2705A.toInt(),
        0xFFF2A93B.toInt(), 0xFF4FBF8B.toInt(), 0xFF38B6C9.toInt(), 0xFF8A94A8.toInt(),
    )

    private fun prefs(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    fun done(c: Context) = prefs(c).getBoolean("onboarded", false)
    fun name(c: Context) = prefs(c).getString("profile_name", "").orEmpty()
    fun colorIndex(c: Context) = prefs(c).getInt("profile_color", 0).coerceIn(0, COLORS.lastIndex)
    fun save(c: Context, name: String, color: Int) =
        prefs(c).edit().putString("profile_name", name.trim()).putInt("profile_color", color).putBoolean("onboarded", true).apply()
}

/** First-run flow over Home: welcome, name, icon, three tour pages, done. Also reachable from the clock menu. */
class Onboarding(private val a: Activity, private val onFinish: () -> Unit) {
    private val root = a.findViewById<View>(R.id.onboarding)
    private val dots = a.findViewById<LinearLayout>(R.id.obDots)
    private val icon = a.findViewById<ImageView>(R.id.obIcon)
    private val key = a.findViewById<TextView>(R.id.obKey)
    private val nameField = a.findViewById<EditText>(R.id.obName)
    private val avatars = a.findViewById<LinearLayout>(R.id.obAvatars)
    private val title = a.findViewById<TextView>(R.id.obTitle)
    private val body = a.findViewById<TextView>(R.id.obBody)
    private val back = a.findViewById<TextView>(R.id.obBack)
    private val next = a.findViewById<TextView>(R.id.obNext)
    private val skip = a.findViewById<TextView>(R.id.obSkip)
    private val dp = a.resources.displayMetrics.density
    private val swatches = ArrayList<TextView>()
    private var step = 0
    private var picked = 0
    private var tourOnly = false

    val showing get() = root.visibility == View.VISIBLE

    init {
        icon.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(v: View, o: android.graphics.Outline) = o.setRoundRect(0, 0, v.width, v.height, 14 * dp)
        }
        icon.clipToOutline = true
        repeat(STEPS) {
            dots.addView(View(a).apply {
                layoutParams = LinearLayout.LayoutParams((8 * dp).toInt(), (8 * dp).toInt()).apply { marginStart = (4 * dp).toInt(); marginEnd = (4 * dp).toInt() }
            })
        }
        Profile.COLORS.forEachIndexed { n, _ ->
            val v = TextView(a).apply {
                layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt()).apply { marginStart = (5 * dp).toInt(); marginEnd = (5 * dp).toInt() }
                gravity = Gravity.CENTER
                setTextColor(0xFFFFFFFF.toInt()); textSize = 18f
                isFocusable = true; isClickable = true
                nextFocusDownId = R.id.obNext
                setOnClickListener { picked = n; paintSwatches() }
                setOnFocusChangeListener { _, f -> if (f) { picked = n; paintSwatches() } }
            }
            swatches.add(v); avatars.addView(v)
        }
        nameField.nextFocusDownId = R.id.obNext
        next.setOnClickListener { if (step == PERM_NP || step == PERM_KEYS) openSettings(); go(1) }
        back.setOnClickListener { go(-1) }
        skip.setOnClickListener { finish() }
        nameField.setOnEditorActionListener { _, id, _ -> if (id == EditorInfo.IME_ACTION_DONE) { go(1); true } else false }
    }

    /** [tour] true replays only the three tour pages and leaves the profile alone. */
    fun start(tour: Boolean = false) {
        picked = Profile.colorIndex(a)
        nameField.setText(Profile.name(a))
        tourOnly = tour
        root.visibility = View.VISIBLE
        step = if (tour) TOUR_FIRST else 0
        render()
    }

    fun onBack() { if (step > (if (tourOnly) TOUR_FIRST else 0)) go(-1) }

    private fun go(d: Int) {
        hideIme()
        if (d > 0 && step == STEPS - 1) { finish(); return }
        step = (step + d).coerceIn(0, STEPS - 1)
        // nothing to ask for when it is already on
        if ((step == PERM_NP && granted("enabled_notification_listeners")) || (step == PERM_KEYS && granted("enabled_accessibility_services"))) {
            go(if (d == 0) 1 else d); return
        }
        render()
        slideIn(d)
    }

    /** Content eases in from the side the user is heading, each piece a beat after the last. */
    private fun slideIn(d: Int) {
        if (Fx.reduced) return
        listOf<View>(icon, key, nameField, avatars, title, body).filter { it.visibility == View.VISIBLE }.forEachIndexed { n, v ->
            v.animate().cancel()
            v.alpha = 0f; v.translationX = d * 28 * dp
            v.animate().alpha(1f).translationX(0f).setStartDelay(n * 35L).setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.6f)).start()
        }
    }

    private fun granted(key: String) =
        android.provider.Settings.Secure.getString(a.contentResolver, key)?.contains(a.packageName) == true

    private fun openSettings() {
        val action = if (step == PERM_NP) "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS" else android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
        try { a.startActivity(android.content.Intent(action)) } catch (e: Exception) { /* no such screen on this box: Next simply moves on */ }
    }

    private fun finish() {
        hideIme()
        Profile.save(a, if (tourOnly) Profile.name(a) else nameField.text.toString(), picked)
        root.visibility = View.GONE
        onFinish()
    }

    private fun hideIme() =
        (a.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(nameField.windowToken, 0)

    private fun render() {
        icon.visibility = View.GONE; key.visibility = View.GONE; nameField.visibility = View.GONE; avatars.visibility = View.GONE
        val name = nameField.text.toString().trim()
        val (t, b) = when (step) {
            0 -> R.string.ob_welcome_title to R.string.ob_welcome_body
            1 -> R.string.ob_name_title to R.string.ob_name_body
            2 -> R.string.ob_icon_title to R.string.ob_icon_body
            PERM_NP -> R.string.ob_p1_title to R.string.ob_p1_body
            PERM_KEYS -> R.string.ob_p2_title to R.string.ob_p2_body
            5 -> R.string.ob_t1_title to R.string.ob_t1_body
            6 -> R.string.ob_t2_title to R.string.ob_t2_body
            7 -> R.string.ob_t3_title to R.string.ob_t3_body
            else -> R.string.ob_done_title to R.string.ob_done_body
        }
        title.text = a.getString(t) + if (step == STEPS - 1 && name.isNotEmpty() && !tourOnly) ", $name" else ""
        body.setText(b)
        when (step) {
            0 -> icon.apply { setImageResource(R.drawable.ic_launcher); visibility = View.VISIBLE }
            1 -> nameField.visibility = View.VISIBLE
            2 -> { avatars.visibility = View.VISIBLE; paintSwatches() }
            PERM_NP -> icon.apply { setImageResource(R.drawable.ic_note); visibility = View.VISIBLE }
            PERM_KEYS, 7 -> icon.apply { setImageResource(R.drawable.ic_apps); visibility = View.VISIBLE }
            5 -> icon.apply { setImageResource(R.drawable.ic_chevron_up); visibility = View.VISIBLE }
            6 -> key.apply { text = "OK"; setBackgroundResource(R.drawable.avatar_bg); visibility = View.VISIBLE }
            else -> key.apply { text = initial(); background = circle(Profile.COLORS[picked]); visibility = View.VISIBLE }
        }
        for (i in 0 until dots.childCount) dots.getChildAt(i).background = circle(if (i == step) 0xFFFFFFFF.toInt() else 0x55FFFFFF)
        back.visibility = if (step == (if (tourOnly) TOUR_FIRST else 0)) View.GONE else View.VISIBLE
        skip.visibility = if (step == STEPS - 1) View.GONE else View.VISIBLE
        next.setText(when (step) { 0 -> R.string.ob_start; PERM_NP, PERM_KEYS -> R.string.ob_allow; STEPS - 1 -> R.string.ob_done; else -> R.string.ob_next })
        when (step) { 1 -> nameField; 2 -> swatches[picked]; else -> next }.requestFocus()
    }

    private fun initial() = nameField.text.toString().trim().take(1).uppercase().ifEmpty { a.getString(R.string.profile_initial) }

    private fun paintSwatches() {
        swatches.forEachIndexed { n, v ->
            v.text = if (n == picked) initial() else ""
            v.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(Profile.COLORS[n])
                if (n == picked) setStroke((3 * dp).toInt(), 0xFFFFFFFF.toInt())
            }
        }
    }

    private fun circle(c: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c) }

    companion object { const val STEPS = 9; const val TOUR_FIRST = 5; const val PERM_NP = 3; const val PERM_KEYS = 4 }
}
