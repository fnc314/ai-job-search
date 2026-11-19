package dev.fnc314.aijobtracker.frontend.core.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CompanyMetricsTest {

    @Test
    fun `fromMap parses interviewing counts and percentages correctly`() {
        val map = mapOf(
            "counts" to mapOf(
                "total" to 5,
                "draft" to 1,
                "submitted" to 2,
                "interviewing" to 1,
                "offer" to 1,
                "rejected" to 0,
            ),
            "percentages" to mapOf(
                "draft" to 0.2f,
                "submitted" to 0.4f,
                "interviewing" to 0.2f,
                "offer" to 0.2f,
                "rejected" to 0.0f,
            ),
        )

        val metrics = CompanyMetrics.fromMap(map)
        assertNotNull(metrics)
        assertEquals(5, metrics.counts.total)
        assertEquals(1, metrics.counts.interview)
        assertEquals(1, metrics.counts.offer)
        assertEquals(0.2f, metrics.percentages.interview)
        assertEquals(0.2f, metrics.percentages.offer)
    }

    @Test
    fun `fromMap falls back to interview key for legacy data`() {
        val map = mapOf(
            "counts" to mapOf(
                "total" to 3,
                "draft" to 0,
                "submitted" to 1,
                "interview" to 2,
                "offer" to 0,
                "rejected" to 0,
            ),
            "percentages" to mapOf(
                "draft" to 0.0f,
                "submitted" to 0.33f,
                "interview" to 0.67f,
                "offer" to 0.0f,
                "rejected" to 0.0f,
            ),
        )

        val metrics = CompanyMetrics.fromMap(map)
        assertNotNull(metrics)
        assertEquals(3, metrics.counts.total)
        assertEquals(2, metrics.counts.interview)
        assertEquals(0.67f, metrics.percentages.interview)
    }

    @Test
    fun `fromMap prefers interviewing over interview when both are present`() {
        val map = mapOf(
            "counts" to mapOf(
                "total" to 10,
                "draft" to 2,
                "submitted" to 3,
                "interviewing" to 4,
                "interview" to 99,
                "offer" to 1,
                "rejected" to 0,
            ),
            "percentages" to mapOf(
                "draft" to 0.2f,
                "submitted" to 0.3f,
                "interviewing" to 0.4f,
                "interview" to 0.99f,
                "offer" to 0.1f,
                "rejected" to 0.0f,
            ),
        )

        val metrics = CompanyMetrics.fromMap(map)
        assertNotNull(metrics)
        assertEquals(4, metrics.counts.interview)
        assertEquals(0.4f, metrics.percentages.interview)
    }

    @Test
    fun `fromMap returns null for null input`() {
        val metrics = CompanyMetrics.fromMap(null)
        assertEquals(null, metrics)
    }

    @Test
    fun `fromMap defaults to zero when interview keys are missing`() {
        val map = mapOf(
            "counts" to mapOf(
                "total" to 2,
                "draft" to 1,
                "submitted" to 1,
                "offer" to 0,
                "rejected" to 0,
            ),
            "percentages" to mapOf(
                "draft" to 0.5f,
                "submitted" to 0.5f,
                "offer" to 0.0f,
                "rejected" to 0.0f,
            ),
        )

        val metrics = CompanyMetrics.fromMap(map)
        assertNotNull(metrics)
        assertEquals(0, metrics.counts.interview)
        assertEquals(0f, metrics.percentages.interview)
    }
}
