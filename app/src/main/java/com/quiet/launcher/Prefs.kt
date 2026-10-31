package com.quiet.launcher

import android.content.Context

/** Stores which apps are on the home screen, their order and custom names. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("quiet", Context.MODE_PRIVATE)

    var configured: Boolean
        get() = sp.getBoolean("configured", false)
        set(v) = sp.edit().putBoolean("configured", v).apply()

    /** Ordered list of app keys shown on the home screen. */
    var homeApps: List<String>
        get() = sp.getString("home_apps", "")!!.split('\n').filter { it.isNotBlank() }
        set(v) = sp.edit().putString("home_apps", v.joinToString("\n")).apply()

    fun customName(key: String): String? = sp.getString("name:$key", null)

    fun setCustomName(key: String, name: String?) {
        val e = sp.edit()
        if (name.isNullOrBlank()) e.remove("name:$key") else e.putString("name:$key", name.trim())
        e.apply()
    }
}
