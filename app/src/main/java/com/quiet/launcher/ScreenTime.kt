package com.quiet.launcher

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/** Total time spent in apps today (not counting this home screen). Needs "Usage access". */
object ScreenTime {

    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun todayMs(context: Context): Long {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return 0
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(start, now) ?: return 0
        val e = UsageEvents.Event()
        val open = HashMap<String, Long>()
        var total = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.packageName == context.packageName) continue
            val k = e.packageName + "/" + e.className
            when (e.eventType) {
                1 -> open[k] = e.timeStamp // activity resumed
                2 -> open.remove(k)?.let { total += e.timeStamp - it } // activity paused
            }
        }
        open.values.maxOrNull()?.let { total += now - it }
        return total
    }

    fun format(ms: Long): String {
        val mins = ms / 60_000
        val h = mins / 60
        val m = mins % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
