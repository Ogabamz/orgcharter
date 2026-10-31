package com.quiet.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** One launchable app. [key] is "package/activity" and is what we store. */
data class AppEntry(val key: String, val label: String, val packageName: String)

object Apps {

    /** Never offered for the home screen, never shown there. */
    private val hiddenPackages = setOf(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.kids",
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
    )

    fun isHidden(pkg: String) = pkg in hiddenPackages

    /** Every app that has a launcher entry, except this launcher itself. */
    fun installed(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .map {
                AppEntry(
                    key = ComponentName(it.packageName, it.name).flattenToString(),
                    label = it.loadLabel(pm).toString().trim(),
                    packageName = it.packageName,
                )
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun label(context: Context, pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
    } catch (e: Exception) {
        pkg
    }

    fun launch(context: Context, key: String): Boolean {
        val component = ComponentName.unflattenFromString(key) ?: return false
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun matches(app: AppEntry, words: List<String>): Boolean {
        val hay = (app.label + " " + app.packageName).lowercase()
        return words.any { hay.contains(it) }
    }

    // Used only to pre-tick likely apps. You can untick anything.
    private val homeWords = listOf(
        "whatsapp", "phone", "dialer", "messages", "mms", "contacts",
        "bible", "pdf", "wps", "office",
        "camera",
        "bank", "opay", "palmpay", "kuda", "moniepoint", "gtworld", "firstmobile", "alat",
        "chatgpt", "openai", "claude", "anthropic", "gemini", "copilot", "perplexity", "deepseek", "grok", "meta ai",
    )

    fun looksSuggested(app: AppEntry) = matches(app, homeWords)

    private val bedtimeWords = listOf("phone", "dialer", "bible", "clock", "alarm")

    fun looksBedtime(app: AppEntry) = matches(app, bedtimeWords)

    private val distractingWords = listOf(
        "youtube", "tiktok", "musically", "com.ss.android.ugc", "instagram", "facebook.katana",
        "twitter", "snapchat", "netflix", "reddit", "likee", "threads",
    )

    private fun defaultDistracting(installed: List<AppEntry>): Set<String> =
        installed.filter { matches(it, distractingWords) }
            .map { it.packageName }
            .filter { it != "com.google.android.apps.youtube.music" }
            .toSet() + "com.google.android.youtube"

    /** Packages that get the pause screen, set up with sensible defaults the first time. */
    fun distracting(context: Context, prefs: Prefs): Set<String> =
        prefs.distracting ?: defaultDistracting(installed(context)).also { prefs.distracting = it }

    fun bedtimeApps(context: Context, prefs: Prefs, installed: List<AppEntry>): List<String> =
        if (prefs.hasBedtimeApps) prefs.bedtimeApps
        else installed.filter { looksBedtime(it) && !isHidden(it.packageName) }.map { it.key }
            .also { prefs.bedtimeApps = it }

    fun findBible(context: Context, prefs: Prefs): String? {
        val all = installed(context)
        val bibles = all.filter { matches(it, listOf("bible")) }
        return (bibles.firstOrNull { it.key in prefs.homeApps } ?: bibles.firstOrNull())?.key
    }
}
