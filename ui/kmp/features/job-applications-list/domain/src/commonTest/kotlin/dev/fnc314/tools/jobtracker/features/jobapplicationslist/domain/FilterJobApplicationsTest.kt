package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FilterJobApplicationsTest {

    private val now = Instant.parse("2026-11-04T12:00:00Z")

    @Test
    fun testFilterByStatus() {
        val app1 = Factory.createJobApplication(id = "1", company = "Company A", status = TrackerStatus.APPLIED)
        val app2 = Factory.createJobApplication(id = "2", company = "Company B", status = TrackerStatus.INTERVIEW)
        val app3 = Factory.createJobApplication(id = "3", company = "Company C", status = TrackerStatus.REJECTED)

        val params = FilterJobApplicationsUseCase.Params(
            applications = listOf(app1, app2, app3),
            status = TrackerStatus.INTERVIEW,
        )
        val filtered = params.filter(now = now)
        assertEquals(listOf("2"), filtered.map { it.id })
    }

    @Test
    fun testFilterByAge() {
        val recent = Factory.createJobApplication(id = "recent", company = "Recent Co", createdAt = now - 3.days)
        val older = Factory.createJobApplication(id = "older", company = "Older Co", createdAt = now - 20.days)
        val ancient = Factory.createJobApplication(id = "ancient", company = "Ancient Co", createdAt = now - 400.days)

        val paramsWeek = FilterJobApplicationsUseCase.Params(
            applications = listOf(recent, older, ancient),
            age = FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_WEEK,
        )
        assertEquals(listOf("recent"), paramsWeek.filter(now = now).map { it.id })

        val paramsYear = FilterJobApplicationsUseCase.Params(
            applications = listOf(recent, older, ancient),
            age = FilterJobApplicationsUseCase.ApplicationAge.MORE_THAN_1_YEAR,
        )
        assertEquals(listOf("ancient"), paramsYear.filter(now = now).map { it.id })
    }

    @Test
    fun testFilterNoneReturnsAll() {
        val app1 = Factory.createJobApplication(id = "1", company = "Company 1", status = TrackerStatus.APPLIED)
        val app2 = Factory.createJobApplication(id = "2", company = "Company 2", status = TrackerStatus.OFFER_EXPECTED)

        val params = FilterJobApplicationsUseCase.Params(
            applications = listOf(app1, app2),
        )
        assertEquals(listOf("1", "2"), params.filter(now = now).map { it.id })
    }

    @Test
    fun testAgeFilter_ExactBoundaryCondition() {
        // Exactly 7 days ago (elapsed == 7.days). Under elapsed < 7.days, this must be FALSE.
        val exactBoundary = now - 7.days
        assertFalse(
            FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_WEEK.matches(exactBoundary, now),
            "Exact 7 days threshold must NOT match LESS_THAN_1_WEEK",
        )

        // 1 second before 7 days (elapsed < 7.days). Must be TRUE.
        val justUnderBoundary = now - (7.days - 1.seconds)
        assertTrue(
            FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_WEEK.matches(justUnderBoundary, now),
            "6 days 23 hours 59 seconds must match LESS_THAN_1_WEEK",
        )
    }

    @Test
    fun testAgeFilter_FutureTimestampNegativeDuration() {
        // An application timestamped in the future produces negative elapsed duration
        val future = now + 2.days
        assertTrue(
            FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_WEEK.matches(future, now),
            "Future timestamp has negative elapsed time and should match LESS_THAN_1_WEEK",
        )
        assertFalse(
            FilterJobApplicationsUseCase.ApplicationAge.MORE_THAN_1_YEAR.matches(future, now),
            "Future timestamp must NOT match MORE_THAN_1_YEAR",
        )
    }

    @Test
    fun testAgeFilter_NullCreatedAtHandling() {
        // Null createdAt must return false for all specific age windows, but true for NONE
        assertTrue(FilterJobApplicationsUseCase.ApplicationAge.NONE.matches(null, now))
        assertFalse(FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_WEEK.matches(null, now))
        assertFalse(FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_MONTH.matches(null, now))
        assertFalse(FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_YEAR.matches(null, now))
        assertFalse(FilterJobApplicationsUseCase.ApplicationAge.MORE_THAN_1_YEAR.matches(null, now))
    }

    @Test
    fun testAgeFilter_YearThresholds() {
        val exact365Days = now - 365.days
        // At 365 days, elapsed < 365.days is false, but elapsed >= 365.days is true
        assertFalse(FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_YEAR.matches(exact365Days, now))
        assertTrue(FilterJobApplicationsUseCase.ApplicationAge.MORE_THAN_1_YEAR.matches(exact365Days, now))

        val day364 = now - 364.days
        assertTrue(FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_YEAR.matches(day364, now))
        assertFalse(FilterJobApplicationsUseCase.ApplicationAge.MORE_THAN_1_YEAR.matches(day364, now))
    }

    @Test
    fun testFilter_WildcardStatusBypass() {
        val app1 = Factory.createJobApplication(id = "1", company = "A", status = TrackerStatus.APPLIED)
        val app2 = Factory.createJobApplication(id = "2", company = "B", status = TrackerStatus.REJECTED)
        val list = listOf(app1, app2)

        val paramsNone = FilterJobApplicationsUseCase.Params(applications = list, status = TrackerStatus.NONE)
        assertEquals(2, paramsNone.filter(now = now).size)

        val paramsUnspecified = FilterJobApplicationsUseCase.Params(applications = list, status = TrackerStatus.UNSPECIFIED)
        assertEquals(2, paramsUnspecified.filter(now = now).size)
    }

    @Test
    fun testFilter_ContradictoryCriteriaEmptyIntersection() {
        // Status matches INTERVIEW, but age is ancient (> 400 days)
        val ancientInterview = Factory.createJobApplication(
            id = "ancient-interview",
            company = "Old Corp",
            status = TrackerStatus.INTERVIEW,
            createdAt = now - 400.days,
        )
        val params = FilterJobApplicationsUseCase.Params(
            applications = listOf(ancientInterview),
            status = TrackerStatus.INTERVIEW,
            age = FilterJobApplicationsUseCase.ApplicationAge.LESS_THAN_1_WEEK,
        )
        val result = params.filter(now = now)
        assertTrue(result.isEmpty(), "Contradictory status/age filter must return empty list")
    }

    @Test
    fun testFilter_PreservesOriginalListOrderingAndEmptyInput() {
        val emptyParams = FilterJobApplicationsUseCase.Params(
            applications = emptyList(),
            status = TrackerStatus.APPLIED,
        )
        assertTrue(emptyParams.filter(now = now).isEmpty())

        val app1 = Factory.createJobApplication(id = "z-first", company = "Zebra", status = TrackerStatus.APPLIED)
        val app2 = Factory.createJobApplication(id = "a-second", company = "Alpha", status = TrackerStatus.APPLIED)
        val params = FilterJobApplicationsUseCase.Params(
            applications = listOf(app1, app2),
            status = TrackerStatus.APPLIED,
        )
        assertEquals(listOf("z-first", "a-second"), params.filter(now = now).map { it.id })
    }
}
