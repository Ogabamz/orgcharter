package com.quiet.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

data class Note(val text: String, val at: Long)

/** Everything Quiet remembers, kept in one small preferences file. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("quiet", Context.MODE_PRIVATE)

    private fun list(key: String): List<String> =
        sp.getString(key, "")!!.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    private fun putList(key: String, v: Collection<String>) =
        sp.edit().putString(key, v.joinToString("\n")).apply()

    // ---- home screen ----

    var configured: Boolean
        get() = sp.getBoolean("configured", false)
        set(v) = sp.edit().putBoolean("configured", v).apply()

    /** Ordered app keys ("package/activity") shown on the home screen. */
    var homeApps: List<String>
        get() = list("home_apps")
        set(v) = putList("home_apps", v)

    fun customName(key: String): String? = sp.getString("name:$key", null)

    fun setCustomName(key: String, name: String?) {
        val e = sp.edit()
        if (name.isNullOrBlank()) e.remove("name:$key") else e.putString("name:$key", name.trim())
        e.apply()
    }

    /** Today's one thing. Clears itself when the day changes. */
    var intention: String?
        get() = if (sp.getString("intention_date", null) == today()) {
            sp.getString("intention", null)?.takeIf { it.isNotBlank() }
        } else null
        set(v) = sp.edit().putString("intention", v?.trim()).putString("intention_date", today()).apply()

    // ---- distractions ----

    /** Packages that get a pause screen. Null until first set up. */
    var distracting: Set<String>?
        get() = if (sp.contains("distracting")) list("distracting").toSet() else null
        set(v) = putList("distracting", v ?: emptySet())

    var pauseSeconds: Int
        get() = sp.getInt("pause_seconds", 10)
        set(v) = sp.edit().putInt("pause_seconds", v).apply()

    var blockedSites: List<String>
        get() = if (sp.contains("blocked_sites")) list("blocked_sites") else DEFAULT_SITES
        set(v) = putList("blocked_sites", v)

    fun opensToday(pkg: String): Int = sp.getInt("opens:${today()}:$pkg", 0)

    fun addOpen(pkg: String): Int {
        val n = opensToday(pkg) + 1
        sp.edit().putInt("opens:${today()}:$pkg", n).apply()
        return n
    }

    // ---- focus timer ----

    var focusMinutes: Int
        get() = sp.getInt("focus_minutes", 25)
        set(v) = sp.edit().putInt("focus_minutes", v).apply()

    /** "idle", "focus" or "break". */
    var pomoPhase: String
        get() = sp.getString("pomo_phase", "idle")!!
        set(v) = sp.edit().putString("pomo_phase", v).apply()

    var pomoEnd: Long
        get() = sp.getLong("pomo_end", 0L)
        set(v) = sp.edit().putLong("pomo_end", v).apply()

    fun sessionsOn(day: LocalDate): Int = sp.getInt("pomo:$day", 0)

    fun addSession() {
        val key = "pomo:${LocalDate.now()}"
        sp.edit().putInt(key, sp.getInt(key, 0) + 1).apply()
    }

    /** Days in a row with at least one finished focus session (today may still be pending). */
    fun streak(): Int {
        var day = LocalDate.now()
        if (sessionsOn(day) == 0) day = day.minusDays(1)
        var n = 0
        while (n < 3650 && sessionsOn(day) > 0) {
            n++
            day = day.minusDays(1)
        }
        return n
    }

    // ---- bedtime ----

    var bedtimeEnabled: Boolean
        get() = sp.getBoolean("bedtime_on", false)
        set(v) = sp.edit().putBoolean("bedtime_on", v).apply()

    /** Minutes after midnight. */
    var bedtimeStart: Int
        get() = sp.getInt("bedtime_start", 22 * 60)
        set(v) = sp.edit().putInt("bedtime_start", v).apply()

    var bedtimeEnd: Int
        get() = sp.getInt("bedtime_end", 6 * 60)
        set(v) = sp.edit().putInt("bedtime_end", v).apply()

    val hasBedtimeApps: Boolean get() = sp.contains("bedtime_apps")

    var bedtimeApps: List<String>
        get() = list("bedtime_apps")
        set(v) = putList("bedtime_apps", v)

    fun isBedtimeNow(now: LocalTime = LocalTime.now()): Boolean {
        if (!bedtimeEnabled) return false
        val m = now.hour * 60 + now.minute
        val s = bedtimeStart
        val e = bedtimeEnd
        return when {
            s == e -> false
            s < e -> m in s until e
            else -> m >= s || m < e
        }
    }

    // ---- notes ----

    var notes: List<Note>
        get() {
            val raw = sp.getString("notes", null) ?: return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    Note(o.getString("t"), o.getLong("at"))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(JSONObject().put("t", it.text).put("at", it.at)) }
            sp.edit().putString("notes", arr.toString()).apply()
        }

    companion object {
        val DEFAULT_SITES = listOf("youtube.com", "youtu.be")
        fun today(): String = LocalDate.now().toString()
    }
}
