package com.quiet.launcher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.format.DateFormat
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Everything you can change. Long-press the clock to get here. */
class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var box: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val scroll = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.bg))
            fitsSystemWindows = true
        }
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = Ui.dp(this@SettingsActivity, 24)
            setPadding(p, p, p, p)
        }
        scroll.addView(box)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun build() {
        box.removeAllViews()
        box.addView(Ui.text(this, "Settings", 30f, face = Ui.serifLight(this)))

        section("Home")
        row("Home apps", "${prefs.homeApps.size} apps on your home screen") { picker(AppPickerActivity.HOME) }

        section("Focus")
        row("Focus length", "${prefs.focusMinutes} minutes, then a ${Pomodoro.BREAK_MINUTES}-minute break") {
            choose("Focus length", listOf(15, 25, 30, 45, 50, 60), { "$it minutes" }) { prefs.focusMinutes = it }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            row("Timer alerts", if (ok) "On" else "Off — tap to allow notifications") {
                if (!ok) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                else safeStart(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
        }

        section("Distractions")
        val guardOn = GuardService.isEnabled(this)
        row("Pause & blocking", if (guardOn) "On" else "Off — tap to turn on (needed for the two below)") { guardSetup() }
        row("Pause before opening", describeApps(Apps.distracting(this, prefs))) { picker(AppPickerActivity.DISTRACTING) }
        row("Pause length", "${prefs.pauseSeconds} seconds") {
            choose("Pause length", listOf(5, 10, 20, 30, 60), { "$it seconds" }) { prefs.pauseSeconds = it }
        }
        row("Blocked websites", prefs.blockedSites.joinToString(", ").ifEmpty { "None" }) { editSites() }
        row("Screen time", if (ScreenTime.hasAccess(this)) "Shown under the date" else "Off — tap to allow usage access") {
            safeStart(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        row("Grayscale", if (Grayscale.canControl(this)) "Ready — tap “mono” on the home screen" else "Needs a one-time setup") {
            Grayscale.showSetup(this)
        }

        section("Bedtime")
        row("Bedtime mode", if (prefs.bedtimeEnabled) "On, ${fmt(prefs.bedtimeStart)} – ${fmt(prefs.bedtimeEnd)}" else "Off") {
            prefs.bedtimeEnabled = !prefs.bedtimeEnabled
            build()
        }
        row("Starts", fmt(prefs.bedtimeStart)) { pickTime(prefs.bedtimeStart) { prefs.bedtimeStart = it } }
        row("Ends", fmt(prefs.bedtimeEnd)) { pickTime(prefs.bedtimeEnd) { prefs.bedtimeEnd = it } }
        val bedtimeCount = Apps.bedtimeApps(this, prefs, Apps.installed(this)).size
        row("Bedtime apps", "$bedtimeCount apps — the only ones shown at bedtime") { picker(AppPickerActivity.BEDTIME) }

        section("Phone")
        row("Set Quiet as default home app", "") { openDefaultHomeSettings() }
        row("Phone settings", "") { safeStart(Intent(Settings.ACTION_SETTINGS)) }

        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            ""
        }
        box.addView(Ui.text(this, "Quiet $version", 12f, R.color.dim).apply {
            gravity = Gravity.CENTER
            setPadding(0, Ui.dp(this@SettingsActivity, 32), 0, 0)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun section(name: String) {
        box.addView(Ui.text(this, name.uppercase(), 12f, R.color.dim).apply {
            letterSpacing = 0.12f
            setPadding(0, Ui.dp(this@SettingsActivity, 28), 0, Ui.dp(this@SettingsActivity, 4))
        })
    }

    private fun row(title: String, subtitle: String, onClick: () -> Unit) {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = Ui.dp(this@SettingsActivity, 12)
            setPadding(0, p, 0, p)
            Ui.clickable(this)
            setOnClickListener { onClick() }
        }
        r.addView(Ui.text(this, title, 19f))
        if (subtitle.isNotEmpty()) r.addView(Ui.text(this, subtitle, 13f, R.color.dim))
        box.addView(r, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun describeApps(pkgs: Set<String>): String {
        if (pkgs.isEmpty()) return "No apps"
        val names = pkgs.map { Apps.label(this, it) }.sorted()
        return if (names.size <= 3) names.joinToString(", ")
        else names.take(3).joinToString(", ") + " and ${names.size - 3} more"
    }

    private fun picker(mode: String) =
        startActivity(Intent(this, AppPickerActivity::class.java).putExtra(AppPickerActivity.EXTRA_MODE, mode))

    private fun <T> choose(title: String, options: List<T>, label: (T) -> String, onPick: (T) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options.map(label).toTypedArray()) { _, which ->
                onPick(options[which])
                build()
            }
            .show()
    }

    private fun pickTime(current: Int, onPick: (Int) -> Unit) {
        TimePickerDialog(this, { _, h, m ->
            onPick(h * 60 + m)
            build()
        }, current / 60, current % 60, DateFormat.is24HourFormat(this)).show()
    }

    private fun fmt(minutes: Int): String {
        val pattern = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        return LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern(pattern))
    }

    private fun editSites() {
        val input = EditText(this).apply {
            setText(prefs.blockedSites.joinToString("\n"))
            hint = "one website per line, e.g. youtube.com"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_VARIATION_URI
            minLines = 4
            gravity = Gravity.TOP or Gravity.START
        }
        val wrap = LinearLayout(this).apply {
            val p = Ui.dp(this@SettingsActivity, 20)
            setPadding(p, Ui.dp(this@SettingsActivity, 8), p, 0)
            addView(input, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setTitle("Blocked websites")
            .setMessage("Blocks the site and everything under it, in Chrome and most other browsers.")
            .setView(wrap)
            .setPositiveButton("Save") { _, _ ->
                prefs.blockedSites = input.text.toString().split('\n', ',', ' ')
                    .mapNotNull { GuardService.hostOf(it) }
                    .distinct()
                build()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun guardSetup() {
        val msg = """
            Quiet uses an accessibility service to notice when you open a paused app or a blocked website. It only looks at which app is open and the browser's address bar. Nothing is saved or sent anywhere.

            On your Redmi:
            1. Tap "Open accessibility" → Downloaded apps (or Installed services) → Quiet → turn it on.
            2. If the switch is greyed out or says "Restricted setting": tap "App info" below → ⋮ (top right) → "Allow restricted settings", then try step 1 again.
            3. In App info, also turn on "Autostart" so Xiaomi doesn't stop it.
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("Turn on pause & blocking")
            .setMessage(msg)
            .setPositiveButton("Open accessibility") { _, _ -> safeStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .setNeutralButton("App info") { _, _ ->
                safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
            .setNegativeButton("Close", null)
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

    private fun safeStart(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}
