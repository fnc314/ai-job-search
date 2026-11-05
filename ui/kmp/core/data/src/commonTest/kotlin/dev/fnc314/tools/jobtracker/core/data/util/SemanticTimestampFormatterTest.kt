package dev.fnc314.tools.jobtracker.core.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SemanticTimestampFormatterTest {

    private val now = Instant.parse("2026-11-05T12:00:00Z")

    @Test
    fun testTimestampPrecedence_UpdatedOverCreated() {
        val created = now - 10.days
        val updated = now - 2.days

        val resultUpdated = formatSemanticTimestamp(updatedAt = updated, createdAt = created, now = now)
        assertEquals("Updated 2 days ago", resultUpdated)

        val resultCreatedOnly = formatSemanticTimestamp(updatedAt = null, createdAt = created, now = now)
        assertEquals("Created 1 week and 3 days ago", resultCreatedOnly)

        val resultBothNull = formatSemanticTimestamp(updatedAt = null, createdAt = null, now = now)
        assertNull(resultBothNull)
    }

    @Test
    fun testTimestamp_BoundaryJustNow() {
        val thirtySecondsAgo = now - 30.seconds
        val oneMinuteAgo = now - 1.minutes

        assertEquals("just now", formatSemanticInstant(thirtySecondsAgo, now))
        assertEquals("just now", formatSemanticInstant(oneMinuteAgo, now))
    }

    @Test
    fun testTimestamp_HourAndDayThresholds() {
        val oneHourAgo = now - 1.hours
        val fiveHoursAgo = now - 5.hours
        val oneDayAgo = now - 24.hours
        val threeDaysAgo = now - 72.hours

        assertEquals("1 hour ago", formatSemanticInstant(oneHourAgo, now))
        assertEquals("5 hours ago", formatSemanticInstant(fiveHoursAgo, now))
        assertEquals("1 day ago", formatSemanticInstant(oneDayAgo, now))
        assertEquals("3 days ago", formatSemanticInstant(threeDaysAgo, now))
    }

    @Test
    fun testTimestamp_WeeksAndDaysBucketing() {
        // Exact 1 week (7 days)
        val oneWeek = now - 7.days
        assertEquals("1 week ago", formatSemanticInstant(oneWeek, now))

        // 1 week and 3 days (10 days)
        val tenDays = now - 10.days
        assertEquals("1 week and 3 days ago", formatSemanticInstant(tenDays, now))

        // Exact 2 weeks (14 days)
        val twoWeeks = now - 14.days
        assertEquals("2 weeks ago", formatSemanticInstant(twoWeeks, now))

        // 3 weeks and 1 day (22 days)
        val twentyTwoDays = now - 22.days
        assertEquals("3 weeks and 1 day ago", formatSemanticInstant(twentyTwoDays, now))
    }

    @Test
    fun testTimestamp_DistantDateFallback() {
        // >= 60 days formats as YYYY-MM-DD (X months ago)
        val seventyDays = now - 70.days
        val formatted = formatSemanticInstant(seventyDays, now)
        assertTrue(formatted.contains("months ago"), "Expected 'months ago' in: $formatted")
        assertTrue(formatted.startsWith("2026-"), "Expected ISO date prefix in: $formatted")
    }
}
