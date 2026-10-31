package com.quiet.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast

/**
 * Turns the whole screen black and white using Android's built-in colour correction.
 * Needs WRITE_SECURE_SETTINGS, which can only be granted once from a computer (adb).
 */
object Grayscale {
    private const val ENABLED = "accessibility_display_daltonizer_enabled"
    private const val MODE = "accessibility_display_daltonizer"
    const val ADB_COMMAND = "adb shell pm grant com.quiet.launcher android.permission.WRITE_SECURE_SETTINGS"

    fun canControl(context: Context) =
        context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    fun isOn(context: Context): Boolean {
        val cr = context.contentResolver
        return Settings.Secure.getInt(cr, ENABLED, 0) == 1 && Settings.Secure.getInt(cr, MODE, -1) == 0
    }

    fun set(context: Context, on: Boolean): Boolean = try {
        val cr = context.contentResolver
        if (on) Settings.Secure.putInt(cr, MODE, 0)
        Settings.Secure.putInt(cr, ENABLED, if (on) 1 else 0)
        true
    } catch (e: SecurityException) {
        false
    }

    fun showSetup(activity: Activity) {
        val msg = """
            Grayscale needs a one-time permission from a computer.

            1. Settings → About phone → tap "OS version" 7 times to unlock Developer options.
            2. Settings → Additional settings → Developer options → turn on "USB debugging" and "USB debugging (Security settings)".
            3. Connect the phone to a computer with adb and run:

            $ADB_COMMAND

            Then tap "mono" on the home screen.
        """.trimIndent()
        AlertDialog.Builder(activity)
            .setTitle(if (canControl(activity)) "Grayscale is ready" else "Set up grayscale")
            .setMessage(msg)
            .setPositiveButton("Copy command") { _, _ ->
                val cm = activity.getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(ClipData.newPlainText("adb", ADB_COMMAND))
                Toast.makeText(activity, "Copied", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Use phone settings") { _, _ ->
                try {
                    activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
