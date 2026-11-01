package com.quiet.launcher

import android.content.Context

/**
 * Lite edition: no accessibility service, so Play Protect doesn't block the install.
 * No pause screen and no website blocking. See the full edition for those.
 */
object GuardService {
    fun hasPass(pkg: String) = true
    fun grantPass(pkg: String, minutes: Int) {}
    fun isEnabled(context: Context) = false
}
