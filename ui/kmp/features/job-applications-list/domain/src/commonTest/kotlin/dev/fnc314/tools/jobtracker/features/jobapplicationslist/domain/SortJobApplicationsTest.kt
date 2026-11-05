package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SortJobApplicationsTest {

    private val now = Instant.parse("2026-11-04T12:00:00Z")

    @Test
    fun testSortByFitScore() {
        val app1 = Factory.createJobApplication(id = "1", company = "A", fitRating = 70)
        val app2 = Factory.createJobApplication(id = "2", company = "B", fitRating = 95)
        val app3 = Factory.createJobApplication(id = "3", company = "C", fitRating = 80)

        val paramsDesc = SortJobApplicationsUseCase.Params(
            applications = listOf(app1, app2, app3),
            column = SortJobApplicationsUseCase.SortColumn.FIT,
            ascending = false,
        )
        val sortedDesc = paramsDesc.sort()
        assertEquals(listOf("2", "3", "1"), sortedDesc.map { it.id })

        val paramsAsc = SortJobApplicationsUseCase.Params(
            applications = listOf(app1, app2, app3),
            column = SortJobApplicationsUseCase.SortColumn.FIT,
            ascending = true,
        )
        val sortedAsc = paramsAsc.sort()
        assertEquals(listOf("1", "3", "2"), sortedAsc.map { it.id })
    }

    @Test
    fun testSort_DateCreatedNullTimestampFallback() {
        val nullDate = Factory.createJobApplication(id = "null-date", company = "Zebra", createdAt = null)
        val oldDate = Factory.createJobApplication(id = "old-date", company = "Beta", createdAt = now - 10.days)
        val recentDate = Factory.createJobApplication(id = "recent-date", company = "Alpha", createdAt = now - 1.days)

        val paramsAsc = SortJobApplicationsUseCase.Params(
            applications = listOf(recentDate, nullDate, oldDate),
            column = SortJobApplicationsUseCase.SortColumn.DATE_CREATED,
            ascending = true,
        )
        val sortedAsc = paramsAsc.sort()
        // Null timestamp maps to DISTANT_PAST and must be sorted first in ascending order
        assertEquals(listOf("null-date", "old-date", "recent-date"), sortedAsc.map { it.id })

        val paramsDesc = SortJobApplicationsUseCase.Params(
            applications = listOf(recentDate, nullDate, oldDate),
            column = SortJobApplicationsUseCase.SortColumn.DATE_CREATED,
            ascending = false,
        )
        val sortedDesc = paramsDesc.sort()
        assertEquals(listOf("recent-date", "old-date", "null-date"), sortedDesc.map { it.id })
    }

    @Test
    fun testSort_DateUpdatedFallbackToCreatedAt() {
        val appWithUpdated = Factory.createJobApplication(
            id = "with-updated",
            company = "A",
            createdAt = now - 20.days,
            updatedAt = now - 1.days,
        )
        val appWithOnlyCreated = Factory.createJobApplication(
            id = "only-created",
            company = "B",
            createdAt = now - 5.days,
            updatedAt = null,
        )
        val appWithNeither = Factory.createJobApplication(
            id = "neither",
            company = "C",
            createdAt = null,
            updatedAt = null,
        )

        val paramsAsc = SortJobApplicationsUseCase.Params(
            applications = listOf(appWithUpdated, appWithNeither, appWithOnlyCreated),
            column = SortJobApplicationsUseCase.SortColumn.DATE_UPDATED,
            ascending = true,
        )
        val sorted = paramsAsc.sort()
        // appWithNeither falls back to DISTANT_PAST, only-created falls back to createdAt (now - 5d), with-updated has now - 1d
        assertEquals(listOf("neither", "only-created", "with-updated"), sorted.map { it.id })
    }

    @Test
    fun testSort_CompanyCaseInsensitive() {
        val app1 = Factory.createJobApplication(id = "1", company = "apple")
        val app2 = Factory.createJobApplication(id = "2", company = "Banana")
        val app3 = Factory.createJobApplication(id = "3", company = "cherry")
        val app4 = Factory.createJobApplication(id = "4", company = "ALPHA")

        val params = SortJobApplicationsUseCase.Params(
            applications = listOf(app1, app2, app3, app4),
            column = SortJobApplicationsUseCase.SortColumn.COMPANY,
            ascending = true,
        )
        val sorted = params.sort()
        assertEquals(listOf("4", "1", "2", "3"), sorted.map { it.id })
    }

    @Test
    fun testSort_RoleNullSortsFirst() {
        val appWithNullRole = Factory.createJobApplication(id = "null-role", company = "X", role = null)
        val appWithEngineer = Factory.createJobApplication(id = "engineer", company = "Y", role = "Engineer")
        val appWithArchitect = Factory.createJobApplication(id = "architect", company = "Z", role = "Architect")

        val paramsAsc = SortJobApplicationsUseCase.Params(
            applications = listOf(appWithEngineer, appWithNullRole, appWithArchitect),
            column = SortJobApplicationsUseCase.SortColumn.ROLE,
            ascending = true,
        )
        val sortedAsc = paramsAsc.sort()
        // null role maps to "" and sorts first
        assertEquals(listOf("null-role", "architect", "engineer"), sortedAsc.map { it.id })
    }

    @Test
    fun testSort_EmptyAndSingleElementLists() {
        val emptyParams = SortJobApplicationsUseCase.Params(
            applications = emptyList(),
            column = SortJobApplicationsUseCase.SortColumn.COMPANY,
            ascending = true,
        )
        assertTrue(emptyParams.sort().isEmpty())

        val singleApp = Factory.createJobApplication(id = "single", company = "Solo")
        val singleParams = SortJobApplicationsUseCase.Params(
            applications = listOf(singleApp),
            column = SortJobApplicationsUseCase.SortColumn.COMPANY,
            ascending = false,
        )
        assertEquals(listOf("single"), singleParams.sort().map { it.id })
    }
}
