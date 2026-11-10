package dev.fnc314.tools.jobsearch.records.parser

import dev.fnc314.tools.jobsearch.parser.UrlSanitizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UrlSanitizerTest {

    @Test
    fun testStripUtmParameters() {
        val url = "https://example.com/job?utm_source=linkedin&utm_medium=cpc&utm_campaign=hiring"
        assertEquals("https://example.com/job", UrlSanitizer.cleanUrl(url))
    }

    @Test
    fun testStripTrackingParamsPreservesFunctionalParams() {
        // gh_jid is functional, gh_src is tracking
        val url = "https://www.coinbase.com/careers/positions/8241716?gh_jid=8241716&gh_src=r4rlpx331us"
        assertEquals(
            "https://www.coinbase.com/careers/positions/8241716?gh_jid=8241716",
            UrlSanitizer.cleanUrl(url)
        )
    }

    @Test
    fun testStripClickIds() {
        val url = "https://careers.google.com/jobs/results/12345?fbclid=abc123xyz&gclid=def456"
        assertEquals(
            "https://careers.google.com/jobs/results/12345",
            UrlSanitizer.cleanUrl(url)
        )
    }

    @Test
    fun testPreservesHashFragments() {
        val url = "https://boards.greenhouse.io/company/jobs/12345?utm_source=jobboard#application"
        assertEquals(
            "https://boards.greenhouse.io/company/jobs/12345#application",
            UrlSanitizer.cleanUrl(url)
        )
    }

    @Test
    fun testEmptyOrNone() {
        assertNull(UrlSanitizer.cleanUrl(null))
        assertEquals("", UrlSanitizer.cleanUrl(""))
        assertEquals("", UrlSanitizer.cleanUrl("   "))
    }

    @Test
    fun testCleanUrlUntouchedIfNoTrackers() {
        val url = "https://jobs.lever.co/company/abc-123"
        assertEquals(url, UrlSanitizer.cleanUrl(url))
    }

    @Test
    fun testComplexAndMultipleParams() {
        val url = "https://example.com/search?q=kotlin+developer&page=2&ref=newsletter&utm_term=android"
        assertEquals(
            "https://example.com/search?q=kotlin+developer&page=2",
            UrlSanitizer.cleanUrl(url)
        )
    }

    @Test
    fun testCaseInsensitiveTrackingParams() {
        val url = "https://example.com/job?UTM_SOURCE=google&FBCLID=12345&keep=true"
        assertEquals(
            "https://example.com/job?keep=true",
            UrlSanitizer.cleanUrl(url)
        )
    }
}
