package com.quiet.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** One launchable app. [key] is "package/activity" and is what we store. */
data class AppEntry(val key: String, val label: String, val packageName: String)

object Apps {

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

    // Words used only on first run to pre-tick likely apps. You can untick anything.
    private val suggestionWords = listOf(
        // messaging + phone
        "whatsapp", "phone", "dialer", "messages", "mms", "contacts",
        // faith + documents
        "bible", "pdf", "wps", "office",
        // camera
        "camera",
        // banking / money
        "bank", "opay", "palmpay", "kuda", "moniepoint", "gtworld", "firstmobile", "alat",
        // AI
        "chatgpt", "openai", "claude", "anthropic", "gemini", "copilot", "perplexity", "deepseek", "grok", "meta ai",
    )

    fun looksSuggested(app: AppEntry): Boolean {
        val hay = (app.label + " " + app.packageName).lowercase()
        return suggestionWords.any { hay.contains(it) }
    }
}
