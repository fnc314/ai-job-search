package dev.fnc314.tools.jobtracker.conventions

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * URL sanitation utility: strip tracking, click IDs, referral sources, and telemetry query parameters.
 *
 * Preserves functional query parameters (e.g. `gh_jid` for Greenhouse job IDs).
 * Mirrors and supersedes Python's `scripts/urls.py`.
 */
public object UrlSanitizer {

    /**
     * Case-insensitive set of query parameter keys known to represent tracking or telemetry.
     */
    public val TRACKING_PARAMS: Set<String> = setOf(
        // Google Analytics / Urchin Traffic Monitor
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "utm_id",
        "utm_source_platform",
        "utm_creative_format",
        "utm_marketing_tactic",
        "utm_name",
        // Ad platforms / Click IDs
        "fbclid",
        "gclid",
        "gclsrc",
        "dclid",
        "wbraid",
        "gbraid",
        "msclkid",
        "twclid",
        "ttclid",
        "sc_clickid",
        "yclid",
        "li_fat_id",
        "rb_clickid",
        "wickedid",
        // Job board / ATS tracking & referral sources
        "gh_src", // Greenhouse tracking source (keep gh_jid!)
        "trk", // LinkedIn tracking
        "trkinfo", // LinkedIn tracking info
        "trackingid", // Common ATS tracking ID
        "midtoken", // LinkedIn email tracking token
        "trkemail", // LinkedIn email tracking
        "ref", // Generic referrer
        "ref_id",
        "ref_src",
        "referrer",
        "original_referer",
        "from_page",
        "cm_mmc",
        // Email / Marketing automation
        "mc_cid", // Mailchimp campaign ID
        "mc_eid", // Mailchimp email ID
        "_hsenc", // HubSpot
        "_hsmi", // HubSpot
        "vero_id",
        "mkt_tok", // Marketo tracking token
        // Social media share IDs
        "si", // Spotify / YouTube share tracker
        "igshid", // Instagram share ID
        "share_id"
    )

    /**
     * Strip known tracking and telemetry query parameters from a URL.
     *
     * Preserves functional query parameters (e.g. `gh_jid` for Greenhouse job IDs).
     * Removes trailing '?' when all query parameters are stripped.
     * Returns null if input is null. Returns empty string if input is empty or whitespace.
     */
    @JvmStatic
    public fun cleanUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim() ?: return null
        if (trimmed.isEmpty()) return ""

        val uri = runCatching { URI.create(trimmed) }.getOrNull() ?: return trimmed
        val isCompleteUrl = uri.scheme != null || uri.host != null || uri.rawAuthority != null
        val rawQuery = uri.rawQuery

        if (!isCompleteUrl || rawQuery.isNullOrEmpty()) {
            return trimmed
        }

        val newQuery = filterQueryParameters(rawQuery)
        return reconstructUri(uri, newQuery)
    }

    private fun isTrackingParam(key: String): Boolean {
        val lower = key.lowercase()
        return lower in TRACKING_PARAMS || lower.startsWith("utm_")
    }

    private fun filterQueryParameters(rawQuery: String): String? {
        val pairs = rawQuery.split("&").filter { it.isNotEmpty() }
        val cleaned = pairs.mapNotNull { part ->
            val eqIdx = part.indexOf('=')
            val rawKey = if (eqIdx >= 0) part.substring(0, eqIdx) else part
            val rawVal = if (eqIdx >= 0) part.substring(eqIdx + 1) else ""

            val decodedKey = runCatching {
                URLDecoder.decode(rawKey, StandardCharsets.UTF_8.name())
            }.getOrDefault(rawKey)

            if (isTrackingParam(decodedKey)) {
                null
            } else if (rawVal.isEmpty()) {
                rawKey
            } else {
                "$rawKey=$rawVal"
            }
        }

        return if (cleaned.isNotEmpty()) cleaned.joinToString("&") else null
    }

    private fun reconstructUri(uri: URI, newQuery: String?): String {
        val sb = StringBuilder()
        uri.scheme?.let { sb.append(it).append(":") }
        uri.rawAuthority?.let { sb.append("//").append(it) }
        uri.rawPath?.let { sb.append(it) }
        newQuery?.let { sb.append("?").append(it) }
        uri.rawFragment?.let { sb.append("#").append(it) }
        return sb.toString()
    }
}
