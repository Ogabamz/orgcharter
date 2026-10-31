package com.quiet.launcher

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** Focus timer: N minutes of focus, then a short break. Survives leaving the home screen. */
object Pomodoro {
    const val BREAK_MINUTES = 5
    private const val CH_RUNNING = "timer_running"
    private const val CH_DONE = "timer_done"
    private const val ID_RUNNING = 10
    private const val ID_DONE = 11

    fun isRunning(context: Context) = Prefs(context).pomoPhase != "idle"

    fun start(context: Context, phase: String, minutes: Int) {
        val p = Prefs(context)
        val end = System.currentTimeMillis() + minutes * 60_000L
        p.pomoPhase = phase
        p.pomoEnd = end
        schedule(context, end)
        showRunning(context, phase, end, p.intention)
    }

    fun stop(context: Context) {
        Prefs(context).pomoPhase = "idle"
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
        context.getSystemService(NotificationManager::class.java).cancel(ID_RUNNING)
    }

    /** Moves focus → break → idle when time is up. Safe to call often. */
    fun advanceIfDue(context: Context) {
        val p = Prefs(context)
        val phase = p.pomoPhase
        if (phase == "idle" || System.currentTimeMillis() < p.pomoEnd - 1000) return
        if (phase == "focus") {
            p.addSession()
            notifyDone(context, "Focus session done", "Take a $BREAK_MINUTES-minute break. Look away from the screen.")
            start(context, "break", BREAK_MINUTES)
        } else {
            p.pomoPhase = "idle"
            context.getSystemService(NotificationManager::class.java).cancel(ID_RUNNING)
            notifyDone(context, "Break over", "Ready for another focus session?")
        }
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, PomodoroReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun homeIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 1, Intent(context, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )

    private fun schedule(context: Context, at: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun channels(context: Context): NotificationManager {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_RUNNING, "Focus timer", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Focus timer alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
        )
        return nm
    }

    private fun showRunning(context: Context, phase: String, end: Long, intention: String?) {
        val n = Notification.Builder(context, CH_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(if (phase == "focus") "Focusing" else "On a break")
            .setContentText(intention ?: if (phase == "focus") "Stay with it." else "Rest your eyes.")
            .setWhen(end)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(homeIntent(context))
            .build()
        try {
            channels(context).notify(ID_RUNNING, n)
        } catch (e: SecurityException) {
        }
    }

    private fun notifyDone(context: Context, title: String, text: String) {
        val n = Notification.Builder(context, CH_DONE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(homeIntent(context))
            .build()
        try {
            channels(context).notify(ID_DONE, n)
        } catch (e: SecurityException) {
        }
    }
}

class PomodoroReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Pomodoro.advanceIfDue(context)
    }
}
