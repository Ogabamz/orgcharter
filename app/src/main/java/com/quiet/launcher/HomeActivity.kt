package com.quiet.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * The home screen: a clock, the date, and a short list of app names.
 * No icons, no app drawer. Long-press the clock for settings.
 */
class HomeActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var appList: LinearLayout
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        prefs = Prefs(this)
        appList = findViewById(R.id.appList)
        scroll = findViewById(R.id.scroll)

        val clock = findViewById<View>(R.id.clock)
        clock.setOnClickListener { openAlarms() }
        clock.setOnLongClickListener { showMenu(); true }
        findViewById<View>(R.id.date).setOnClickListener { openCalendar() }
        findViewById<View>(R.id.date).setOnLongClickListener { showMenu(); true }

        if (!prefs.configured) {
            startActivity(Intent(this, AppPickerActivity::class.java).putExtra(AppPickerActivity.EXTRA_FIRST_RUN, true))
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Home pressed while already home: just scroll back to the top.
        scroll.smoothScrollTo(0, 0)
    }

    @Deprecated("Home screen ignores back")
    override fun onBackPressed() {
        // Nothing behind the home screen.
    }

    private fun render() {
        appList.removeAllViews()
        val installed = Apps.installed(this).associateBy { it.key }
        val keys = prefs.homeApps.filter { installed.containsKey(it) }

        if (keys.isEmpty()) {
            appList.addView(TextView(this).apply {
                setText(R.string.empty_home)
                setTextColor(getColor(R.color.dim))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            })
            return
        }

        val pad = dp(12)
        for (key in keys) {
            val app = installed.getValue(key)
            appList.addView(TextView(this).apply {
                text = prefs.customName(key) ?: app.label
                setTextColor(getColor(R.color.fg))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                gravity = Gravity.START
                setPadding(0, pad, 0, pad)
                isSingleLine = true
                setOnClickListener {
                    if (!Apps.launch(this@HomeActivity, key)) {
                        Toast.makeText(this@HomeActivity, "Couldn't open ${app.label}", Toast.LENGTH_SHORT).show()
                    }
                }
                setOnLongClickListener { showAppOptions(app); true }
            })
        }
    }

    private fun showAppOptions(app: AppEntry) {
        val options = arrayOf("Rename", "Move up", "Move down", "Remove from home", "App info")
        AlertDialog.Builder(this)
            .setTitle(prefs.customName(app.key) ?: app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> rename(app)
                    1 -> move(app.key, -1)
                    2 -> move(app.key, +1)
                    3 -> {
                        prefs.homeApps = prefs.homeApps - app.key
                        render()
                    }
                    4 -> startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                    )
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
        val wrap = LinearLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(wrap)
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
        val list = prefs.homeApps.toMutableList()
        val i = list.indexOf(key)
        val j = i + delta
        if (i < 0 || j < 0 || j >= list.size) return
        list[i] = list[j].also { list[j] = list[i] }
        prefs.homeApps = list
        render()
    }

    private fun showMenu() {
        val options = arrayOf("Choose home apps", "Set Quiet as default home app", "Phone settings")
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, AppPickerActivity::class.java))
                    1 -> openDefaultHomeSettings()
                    2 -> safeStart(Intent(Settings.ACTION_SETTINGS))
                }
            }
            .show()
    }

    private fun openDefaultHomeSettings() {
        val attempts = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (i in attempts) if (safeStart(i)) return
    }

    private fun openAlarms() {
        safeStart(Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }

    private fun openCalendar() {
        safeStart(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
    }

    private fun safeStart(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
