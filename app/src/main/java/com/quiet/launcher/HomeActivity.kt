package com.quiet.launcher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.Settings
import android.text.InputType
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

/**
 * The home screen: today's one thing, the clock, a short list of app names,
 * a verse, and three quiet words at the bottom: focus · notes · mono.
 * Long-press the clock for settings. Swipe down for notes.
 */
class HomeActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var appList: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var intentionView: TextView
    private lateinit var screenTimeView: TextView
    private lateinit var bedtimeView: TextView
    private lateinit var verseView: TextView
    private lateinit var verseRefView: TextView
    private lateinit var focusView: TextView
    private lateinit var focusStats: TextView
    private lateinit var monoView: TextView
    private lateinit var gestures: GestureDetector

    private val handler = Handler(Looper.getMainLooper())
    private var showingBedtime: Boolean? = null
    private var ticks = 0
    private val ticker = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        prefs = Prefs(this)
        appList = findViewById(R.id.appList)
        scroll = findViewById(R.id.scroll)
        intentionView = findViewById(R.id.intention)
        screenTimeView = findViewById(R.id.screenTime)
        bedtimeView = findViewById(R.id.bedtime)
        verseView = findViewById(R.id.verse)
        verseRefView = findViewById(R.id.verseRef)
        focusView = findViewById(R.id.focus)
        focusStats = findViewById(R.id.focusStats)
        monoView = findViewById(R.id.mono)

        val clock = findViewById<View>(R.id.clock)
        val date = findViewById<View>(R.id.date)
        clock.setOnClickListener { safeStart(Intent(AlarmClock.ACTION_SHOW_ALARMS)) }
        date.setOnClickListener { safeStart(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)) }
        clock.setOnLongClickListener { openSettings(); true }
        date.setOnLongClickListener { openSettings(); true }

        intentionView.setOnClickListener { editIntention() }
        verseView.setOnClickListener { openBible() }
        verseRefView.setOnClickListener { openBible() }
        screenTimeView.setOnClickListener {
            if (!ScreenTime.hasAccess(this)) safeStart(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        focusView.setOnClickListener { onFocusTap() }
        focusView.setOnLongClickListener { chooseFocusLength(); true }
        findViewById<View>(R.id.notes).setOnClickListener { openNotes() }
        monoView.setOnClickListener { toggleMono() }

        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val start = e1 ?: return false
                val dy = e2.y - start.y
                val dx = e2.x - start.x
                if (dy > Ui.dp(this@HomeActivity, 120) && abs(dy) > abs(dx) * 1.5f && velocityY > 800 && scroll.scrollY == 0) {
                    openNotes()
                    return true
                }
                return false
            }
        })

        if (!prefs.configured) {
            startActivity(Intent(this, AppPickerActivity::class.java).putExtra(AppPickerActivity.EXTRA_MODE, AppPickerActivity.HOME))
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        ticks = 0
        render()
        updateIntention()
        updateVerse()
        updateMono()
        refreshScreenTime()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        scroll.smoothScrollTo(0, 0)
    }

    @Deprecated("Home screen ignores back")
    override fun onBackPressed() {
        // Nothing behind the home screen.
    }

    private fun tick() {
        Pomodoro.advanceIfDue(this)
        updateFocus()
        if (prefs.isBedtimeNow() != showingBedtime) render()
        if (ticks > 0 && ticks % 60 == 0) {
            refreshScreenTime()
            updateIntention()
            updateVerse()
        }
        ticks++
    }

    // ---- app list ----

    private fun currentKeys(): List<String> =
        if (showingBedtime == true) Apps.bedtimeApps(this, prefs, Apps.installed(this)) else prefs.homeApps

    private fun setCurrentKeys(keys: List<String>) {
        if (showingBedtime == true) prefs.bedtimeApps = keys else prefs.homeApps = keys
    }

    private fun render() {
        val bedtime = prefs.isBedtimeNow()
        showingBedtime = bedtime
        bedtimeView.visibility = if (bedtime) View.VISIBLE else View.GONE

        appList.removeAllViews()
        val all = Apps.installed(this)
        val installed = all.associateBy { it.key }
        val keys = (if (bedtime) Apps.bedtimeApps(this, prefs, all) else prefs.homeApps)
            .filter { installed.containsKey(it) && !Apps.isHidden(installed.getValue(it).packageName) }

        if (keys.isEmpty()) {
            val msg = if (bedtime) "Nothing here at bedtime.\nRest well." else getString(R.string.empty_home)
            appList.addView(Ui.text(this, msg, 16f, R.color.dim).apply { gravity = Gravity.CENTER })
            return
        }

        val pad = Ui.dp(this, 11)
        val font = Ui.serif(this)
        for (key in keys) {
            val app = installed.getValue(key)
            appList.addView(Ui.text(this, prefs.customName(key) ?: app.label, 26f, face = font).apply {
                gravity = Gravity.CENTER
                setPadding(0, pad, 0, pad)
                isSingleLine = true
                setOnClickListener {
                    if (!Apps.launch(this@HomeActivity, key)) {
                        Toast.makeText(this@HomeActivity, "Couldn't open ${app.label}", Toast.LENGTH_SHORT).show()
                    }
                }
                setOnLongClickListener { showAppOptions(app); true }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun showAppOptions(app: AppEntry) {
        val options = arrayOf("Rename", "Move up", "Move down", "Remove from this screen", "App info")
        AlertDialog.Builder(this)
            .setTitle(prefs.customName(app.key) ?: app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> rename(app)
                    1 -> move(app.key, -1)
                    2 -> move(app.key, +1)
                    3 -> {
                        setCurrentKeys(currentKeys() - app.key)
                        render()
                    }
                    4 -> safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                }
            }
            .show()
    }

    private fun rename(app: AppEntry) {
        val input = EditText(this).apply {
            setText(prefs.customName(app.key) ?: app.label)
            setSelectAllOnFocus(true)
            isSingleLine = true
        }
        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(padded(input))
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString()
                prefs.setCustomName(app.key, if (name == app.label) null else name)
                render()
            }
            .setNeutralButton("Reset") { _, _ ->
                prefs.setCustomName(app.key, null)
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun move(key: String, delta: Int) {
        val list = currentKeys().toMutableList()
        val i = list.indexOf(key)
        val j = i + delta
        if (i < 0 || j < 0 || j >= list.size) return
        list[i] = list[j].also { list[j] = list[i] }
        setCurrentKeys(list)
        render()
    }

    // ---- intention ----

    private fun updateIntention() {
        val t = prefs.intention
        intentionView.text = t ?: "what’s the one thing today?"
        intentionView.setTextColor(getColor(if (t != null) R.color.fg else R.color.line_text))
    }

    private fun editIntention() {
        val input = EditText(this).apply {
            setText(prefs.intention ?: "")
            hint = "e.g. Finish chapter two"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSingleLine = true
        }
        AlertDialog.Builder(this)
            .setTitle("Today’s one thing")
            .setView(padded(input))
            .setPositiveButton("Save") { _, _ ->
                prefs.intention = input.text.toString()
                updateIntention()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---- verse ----

    private fun updateVerse() {
        val v = Verses.today()
        verseView.text = "“${v.text}”"
        verseRefView.text = v.ref
    }

    private fun openBible() {
        val key = Apps.findBible(this, prefs)
        if (key == null || !Apps.launch(this, key)) {
            Toast.makeText(this, "No Bible app found", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- screen time ----

    private fun refreshScreenTime() {
        if (!ScreenTime.hasAccess(this)) {
            screenTimeView.text = "tap to show screen time"
            return
        }
        Thread {
            val ms = try {
                ScreenTime.todayMs(this)
            } catch (e: Exception) {
                -1L
            }
            runOnUiThread {
                screenTimeView.text = if (ms < 0) "" else "${ScreenTime.format(ms)} on screen today"
            }
        }.start()
    }

    // ---- focus timer ----

    private fun updateFocus() {
        val phase = prefs.pomoPhase
        if (phase == "idle") {
            focusView.text = "focus"
            focusView.setTextColor(getColor(R.color.dim))
        } else {
            val secs = (max(0L, prefs.pomoEnd - System.currentTimeMillis()) + 999) / 1000
            focusView.text = "%s %d:%02d".format(phase, secs / 60, secs % 60)
            focusView.setTextColor(getColor(R.color.fg))
        }
        val today = prefs.sessionsOn(LocalDate.now())
        val streak = prefs.streak()
        val parts = mutableListOf<String>()
        if (today > 0) parts += if (today == 1) "1 focus session today" else "$today focus sessions today"
        if (streak > 1) parts += "$streak-day streak"
        focusStats.text = parts.joinToString(" · ")
        focusStats.visibility = if (parts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun onFocusTap() {
        if (prefs.pomoPhase == "idle") {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            Pomodoro.start(this, "focus", prefs.focusMinutes)
            if (prefs.intention == null) editIntention()
            updateFocus()
        } else {
            AlertDialog.Builder(this)
                .setTitle(if (prefs.pomoPhase == "focus") "End this focus session?" else "End the break?")
                .setPositiveButton("End") { _, _ ->
                    Pomodoro.stop(this)
                    updateFocus()
                }
                .setNegativeButton("Keep going", null)
                .show()
        }
    }

    private fun chooseFocusLength() {
        val options = listOf(15, 25, 30, 45, 50, 60)
        AlertDialog.Builder(this)
            .setTitle("Focus length")
            .setItems(options.map { "$it minutes" }.toTypedArray()) { _, which ->
                prefs.focusMinutes = options[which]
            }
            .show()
    }

    // ---- notes, grayscale, settings ----

    private fun openNotes() = startActivity(Intent(this, NotesActivity::class.java))

    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    private fun updateMono() {
        monoView.setTextColor(getColor(if (Grayscale.canControl(this) && Grayscale.isOn(this)) R.color.fg else R.color.dim))
    }

    private fun toggleMono() {
        if (!Grayscale.canControl(this)) {
            Grayscale.showSetup(this)
            return
        }
        Grayscale.set(this, !Grayscale.isOn(this))
        updateMono()
    }

    // ---- helpers ----

    private fun padded(v: View): View = LinearLayout(this).apply {
        setPadding(Ui.dp(this@HomeActivity, 20), Ui.dp(this@HomeActivity, 8), Ui.dp(this@HomeActivity, 20), 0)
        addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun safeStart(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}
