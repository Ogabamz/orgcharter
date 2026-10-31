package com.quiet.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** The calm screen shown before a distracting app opens, when time is up, or on a blocked website. */
class PauseActivity : Activity() {

    companion object {
        const val EXTRA_TARGET = "target"
        const val EXTRA_MODE = "mode"
        const val MODE_PAUSE = "pause"
        const val MODE_TIMES_UP = "timesup"
        const val MODE_BLOCKED = "blocked"

        @Volatile
        var showing = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private var target = ""
    private var mode = MODE_PAUSE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showing = true
        prefs = Prefs(this)
        build(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        build(intent)
    }

    override fun onStop() {
        super.onStop()
        if (!isFinishing) finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        showing = false
        super.onDestroy()
    }

    @Deprecated("Back goes home")
    override fun onBackPressed() = goHome()

    private fun build(i: Intent) {
        handler.removeCallbacksAndMessages(null)
        target = i.getStringExtra(EXTRA_TARGET) ?: ""
        mode = i.getStringExtra(EXTRA_MODE) ?: MODE_PAUSE
        val name = if (mode == MODE_BLOCKED) target else Apps.label(this, target)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.bg))
            val p = Ui.dp(this@PauseActivity, 32)
            setPadding(p, p, p, p)
        }
        fun add(v: View, top: Int = 0) {
            root.addView(v, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = Ui.dp(this@PauseActivity, top); gravity = Gravity.CENTER_HORIZONTAL })
        }
        fun centered(t: TextView) = t.apply { gravity = Gravity.CENTER }

        val title = when (mode) {
            MODE_BLOCKED -> "$name is blocked"
            MODE_TIMES_UP -> "Your time on $name is up"
            else -> "Open $name?"
        }
        add(centered(Ui.text(this, title, 30f, face = Ui.serifLight(this))))

        val body = when (mode) {
            MODE_BLOCKED -> "You chose to keep this site out of your day."
            MODE_TIMES_UP -> "You said you'd only be a few minutes."
            else -> {
                val n = prefs.opensToday(target)
                if (n <= 1) "First time today." else "That's $n times today."
            }
        }
        add(centered(Ui.text(this, body, 16f, R.color.dim)), 12)

        prefs.intention?.let {
            add(centered(Ui.text(this, "Today: $it", 16f, face = Ui.serifItalic(this))), 28)
        }

        val notNow = centered(Ui.text(this, if (mode == MODE_PAUSE) "Not now" else "Go home", 22f)).apply {
            val p = Ui.dp(this@PauseActivity, 14)
            setPadding(p * 2, p, p * 2, p)
            Ui.clickable(this)
            setOnClickListener { goHome() }
        }

        if (mode == MODE_BLOCKED) {
            add(notNow, 48)
            setContentView(root)
            return
        }

        val countdown = centered(Ui.text(this, "", 15f, R.color.dim))
        add(countdown, 40)
        val choices = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.INVISIBLE
        }
        val options = if (mode == MODE_TIMES_UP) listOf(5) else listOf(5, 15)
        for (m in options) {
            choices.addView(centered(Ui.text(this, if (mode == MODE_TIMES_UP) "$m more minutes" else "Open for $m minutes", 16f, R.color.dim)).apply {
                val p = Ui.dp(this@PauseActivity, 10)
                setPadding(p * 2, p, p * 2, p)
                Ui.clickable(this)
                setOnClickListener { openFor(m) }
            })
        }
        add(choices, 8)
        add(notNow, 32)
        setContentView(root)

        var left = prefs.pauseSeconds
        val tick = object : Runnable {
            override fun run() {
                if (left > 0) {
                    countdown.text = "Take a breath… $left"
                    left--
                    handler.postDelayed(this, 1000)
                } else {
                    countdown.text = "Still want to?"
                    choices.visibility = View.VISIBLE
                }
            }
        }
        handler.post(tick)
    }

    private fun openFor(minutes: Int) {
        GuardService.grantPass(target, minutes)
        val launch = packageManager.getLaunchIntentForPackage(target)
        finish()
        if (launch != null) startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
