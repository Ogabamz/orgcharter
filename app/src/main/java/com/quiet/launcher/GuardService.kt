package com.quiet.launcher

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Watches which app is in front so Quiet can
 *  - show a pause screen before distracting apps, and
 *  - close blocked websites in browsers.
 * It only looks at the app name and the browser's address bar. Nothing is stored or sent anywhere.
 */
class GuardService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: GuardService? = null
        private val passes = ConcurrentHashMap<String, Long>()

        fun hasPass(pkg: String): Boolean = (passes[pkg] ?: 0L) > System.currentTimeMillis()

        fun grantPass(pkg: String, minutes: Int) {
            val ms = minutes * 60_000L
            passes[pkg] = System.currentTimeMillis() + ms
            instance?.checkLater(pkg, ms)
        }

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val me = ComponentName(context, GuardService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        private val knownBrowsers = setOf(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
            "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fenix", "org.mozilla.focus",
            "com.microsoft.emmx", "com.brave.browser", "com.opera.browser", "com.opera.mini.native",
            "com.opera.gx", "com.sec.android.app.sbrowser", "com.mi.globalbrowser", "com.android.browser",
            "com.duckduckgo.mobile.android", "com.vivaldi.browser", "com.kiwibrowser.browser",
            "com.UCMobile.intl", "com.uc.browser.en", "com.transsion.phoenix", "com.yandex.browser",
        )

        private val urlBarIds = listOf(
            "url_bar", "url_field", "mozac_browser_toolbar_url_view", "location_bar_edit_text",
            "url", "address_bar_edit_text", "search_box_text", "url_text",
        )

        /** "https://www.m.youtube.com/watch?v=1" → "m.youtube.com". Null if it doesn't look like a web address. */
        fun hostOf(raw: String): String? {
            var t = raw.trim().lowercase()
            if (t.isEmpty() || t.contains(' ')) return null
            t = t.substringAfter("://")
            t = t.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
            t = t.removePrefix("www.")
            return if (t.contains('.')) t else null
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private var currentPkg: String? = null
    private var lastUrlCheck = 0L
    private var lastBlock = 0L
    private var ignored: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefs = Prefs(this)
        Apps.distracting(this, prefs)
        val imes = try {
            getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }
        } catch (e: Exception) {
            emptyList()
        }
        ignored = setOf("com.android.systemui", "android") + imes
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        if (pkg in ignored) return

        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val prev = currentPkg
            currentPkg = pkg
            if (pkg != prev) onAppChanged(pkg)
        }
        if (isBrowser(pkg)) checkUrl(pkg)
    }

    private fun onAppChanged(pkg: String) {
        if (pkg == packageName) return
        val list = prefs.distracting ?: return
        if (pkg in list && !hasPass(pkg)) showPause(pkg, PauseActivity.MODE_PAUSE)
    }

    fun checkLater(pkg: String, delayMs: Long) {
        handler.postDelayed({
            if (currentPkg == pkg && !hasPass(pkg)) showPause(pkg, PauseActivity.MODE_TIMES_UP)
        }, delayMs + 500)
    }

    private fun showPause(target: String, mode: String) {
        if (PauseActivity.showing) return
        if (mode == PauseActivity.MODE_PAUSE) prefs.addOpen(target)
        val i = Intent(this, PauseActivity::class.java)
            .putExtra(PauseActivity.EXTRA_TARGET, target)
            .putExtra(PauseActivity.EXTRA_MODE, mode)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            startActivity(i)
        } catch (e: Exception) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun isBrowser(pkg: String) =
        pkg in knownBrowsers || pkg.contains("browser") || pkg.contains("chrome") || pkg.contains("firefox")

    private fun checkUrl(pkg: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastUrlCheck < 700 || now - lastBlock < 2500) return
        lastUrlCheck = now
        val sites = prefs.blockedSites
        if (sites.isEmpty()) return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        val host = findUrl(root, pkg)?.let { hostOf(it) } ?: return
        val hit = sites.firstOrNull { host == it || host.endsWith(".$it") } ?: return
        lastBlock = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        showPause(hit, PauseActivity.MODE_BLOCKED)
    }

    private fun findUrl(root: AccessibilityNodeInfo, pkg: String): String? {
        for (id in urlBarIds) {
            val nodes = try {
                root.findAccessibilityNodeInfosByViewId("$pkg:id/$id")
            } catch (e: Exception) {
                null
            } ?: continue
            for (n in nodes) {
                if (n.isFocused) continue // you're typing, not visiting
                val t = n.text?.toString()
                if (!t.isNullOrBlank()) return t
            }
        }
        // Unknown browser: look for anything whose id mentions url/address.
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var seen = 0
        while (queue.isNotEmpty() && seen < 400) {
            val n = queue.removeFirst()
            seen++
            val id = n.viewIdResourceName?.lowercase()
            if (id != null && (id.contains("url") || id.contains("address")) && !n.isFocused) {
                val t = n.text?.toString()
                if (!t.isNullOrBlank() && hostOf(t) != null) return t
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let { queue.add(it) }
        }
        return null
    }
}
