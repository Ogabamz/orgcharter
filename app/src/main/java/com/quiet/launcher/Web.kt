package com.quiet.launcher

object Web {
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
