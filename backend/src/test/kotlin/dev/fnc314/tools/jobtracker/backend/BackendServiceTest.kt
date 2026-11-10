package dev.fnc314.tools.jobtracker.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendServiceTest {

    @Test
    fun `test calculateMetricsForApplications with mixed application records`() {
        val apps = listOf(
            mapOf(
                "status" to "DRAFTING",
                "fitRating" to 80,
                "compensationMin" to 140000.0,
                "compensationMax" to 170000.0,
                "channel" to "LINKEDIN",
                "location" to "Pittsburgh, PA"
            ),
            mapOf(
                "status" to "APPLIED",
                "fitRating" to 90,
                "compensation" to mapOf("min" to 150000.0, "max" to 220000.0, "currency" to "USD"),
                "channel" to "LINKEDIN",
                "location" to "Remote"
            ),
            mapOf(
                "status" to "INTERVIEW",
                "fitRating" to 95,
                "channel" to "REFERRAL",
                "location" to "Pittsburgh, PA" // duplicate location to test dedup
            ),
            mapOf(
                "status" to "OFFER_EXPECTED",
                "fitRating" to 92,
                "compensationMin" to 200000.0,
                "compensationMax" to 210000.0,
                "channel" to "LINKEDIN",
                "location" to "New York, NY"
            ),
            mapOf(
                "status" to "REJECTED",
                "fitRating" to 75,
                "channel" to "DIRECT",
                "location" to "Remote"
            )
        )

        val metrics = BackendService.calculateMetricsForApplications(apps)

        @Suppress("UNCHECKED_CAST")
        val counts = metrics["counts"] as Map<String, Any>
        assertEquals(1, counts["draft"])
        assertEquals(1, counts["submitted"])
        assertEquals(1, counts["interviewing"])
        assertEquals(1, counts["offer"])
        assertEquals(1, counts["rejected"])
        assertEquals(5, counts["total"])

        @Suppress("UNCHECKED_CAST")
        val percentages = metrics["percentages"] as Map<String, Any>
        assertEquals(0.2f, percentages["draft"])
        assertEquals(0.2f, percentages["submitted"])
        assertEquals(0.2f, percentages["interviewing"])
        assertEquals(0.2f, percentages["offer"])
        assertEquals(0.2f, percentages["rejected"])

        // Average Fit Score: (80 + 90 + 95 + 92 + 75) / 5 = 432 / 5 = 86.4 -> 86
        assertEquals(86, metrics["averageFitScore"])

        // Primary channel mode: LINKEDIN (3 out of 5)
        assertEquals("LINKEDIN", metrics["primaryChannel"])

        // Salary range: min 140000, max 220000 -> "$140k - $220k"
        @Suppress("UNCHECKED_CAST")
        val salaryRange = metrics["salaryRange"] as Map<String, Any>
        assertEquals(140000.0, salaryRange["min"])
        assertEquals(220000.0, salaryRange["max"])
        assertEquals("USD", salaryRange["currency"])
        assertEquals("$140k - $220k", salaryRange["display"])

        // Deduplicated locations
        @Suppress("UNCHECKED_CAST")
        val locations = metrics["locations"] as List<String>
        assertEquals(listOf("Pittsburgh, PA", "Remote", "New York, NY"), locations)

        // Timestamp
        assertNotNull(metrics["lastCalculatedAt"])
    }

    @Test
    fun `test calculateMetricsForApplications with empty applications list`() {
        val metrics = BackendService.calculateMetricsForApplications(emptyList())

        @Suppress("UNCHECKED_CAST")
        val counts = metrics["counts"] as Map<String, Any>
        assertEquals(0, counts["total"])

        @Suppress("UNCHECKED_CAST")
        val percentages = metrics["percentages"] as Map<String, Any>
        assertEquals(0f, percentages["draft"])
        assertEquals(0f, percentages["submitted"])

        assertNull(metrics["averageFitScore"])
        assertEquals("DIRECT", metrics["primaryChannel"])

        @Suppress("UNCHECKED_CAST")
        val salaryRange = metrics["salaryRange"] as Map<String, Any>
        assertEquals("", salaryRange["display"])
    }

    @Test
    fun `test calculateMetricsForApplications with single salary`() {
        val apps = listOf(
            mapOf(
                "status" to "APPLIED",
                "compensationMin" to 180000.0,
                "compensationMax" to 180000.0
            )
        )

        val metrics = BackendService.calculateMetricsForApplications(apps)
        @Suppress("UNCHECKED_CAST")
        val salaryRange = metrics["salaryRange"] as Map<String, Any>
        assertEquals("$180k", salaryRange["display"])
    }
}
