package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class SortJobApplicationsTest {

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
}
