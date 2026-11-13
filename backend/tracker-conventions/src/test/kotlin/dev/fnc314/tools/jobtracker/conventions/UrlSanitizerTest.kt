package dev.fnc314.tools.jobtracker.conventions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UrlSanitizerTest {

    @Test
    fun testCleanUrlStripsTrackingParams() {
        val url = "https://example.com/jobs/123?utm_source=google&gh_jid=456&fbclid=abc123"
        val cleaned = UrlSanitizer.cleanUrl(url)
        assertEquals("https://example.com/jobs/123?gh_jid=456", cleaned)
    }

    @Test
    fun testCleanUrlPreservesFunctionalParams() {
        val url = "https://example.com/jobs/123?gh_jid=456&ref=internal"
        val cleaned = UrlSanitizer.cleanUrl(url)
        assertEquals("https://example.com/jobs/123?gh_jid=456", cleaned)
    }

    @Test
    fun testCleanUrlRemovesTrailingQuestionMark() {
        val url = "https://example.com/jobs/123?utm_source=google"
        val cleaned = UrlSanitizer.cleanUrl(url)
        assertEquals("https://example.com/jobs/123", cleaned)
    }

    @Test
    fun testCleanUrlNullInput() {
        assertNull(UrlSanitizer.cleanUrl(null))
    }

    @Test
    fun testCleanUrlEmptyInput() {
        assertEquals("", UrlSanitizer.cleanUrl(""))
    }

    @Test
    fun testCleanUrlNoQueryParams() {
        val url = "https://example.com/jobs/123"
        val cleaned = UrlSanitizer.cleanUrl(url)
        assertEquals("https://example.com/jobs/123", cleaned)
    }

    @Test
    fun testCleanUrlWithFragment() {
        val url = "https://example.com/jobs/123?utm_source=google#section"
        val cleaned = UrlSanitizer.cleanUrl(url)
        assertEquals("https://example.com/jobs/123#section", cleaned)
    }
}
