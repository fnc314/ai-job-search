package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
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
}
