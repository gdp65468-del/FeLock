package com.selflock.app.util

import java.net.URI

object BrowserUrlParser {
    fun extractDomain(url: String): String? {
        return try {
            val normalized = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val uri = URI(normalized)
            uri.host?.removePrefix("www.")?.lowercase()
        } catch (_: Exception) {
            null
        }
    }

    fun isBrowserPackage(packageName: String): Boolean {
        return packageName in setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "org.mozilla.fenix",
            "com.sec.android.app.sbrowser",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.vivaldi.browser",
            "com.duckduckgo.mobile.android"
        )
    }
}
