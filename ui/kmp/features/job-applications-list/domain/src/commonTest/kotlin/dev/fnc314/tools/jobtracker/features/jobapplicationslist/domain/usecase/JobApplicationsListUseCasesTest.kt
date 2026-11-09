package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.repository.JobApplicationsListRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.impl.LoadAllJobApplicationsUseCaseImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JobApplicationsListUseCasesTest {

    private class FakeJobApplicationsListRepository(
        private val apps: List<JobApplication>,
    ) : JobApplicationsListRepository {
        var lastForceRefresh: Boolean? = null
        override suspend operator fun invoke(forceRefresh: Boolean): List<JobApplication> {
            lastForceRefresh = forceRefresh
            return apps
        }
    }

    @Test
    fun testLoadAllJobApplicationsUseCase_ReturnsRepositoryApps() = runTest {
        val testApps = listOf(Factory.createJobApplication(id = "app-repo", company = "Meta"))
        val repo = FakeJobApplicationsListRepository(testApps)
        val useCase = LoadAllJobApplicationsUseCaseImpl(repo)

        val result = useCase(LoadAllJobApplicationsUseCase.Params())
        assertEquals(1, result.size)
        assertEquals("app-repo", result.first().id)
        assertEquals("Meta", result.first().company)
        assertEquals(false, repo.lastForceRefresh)
    }

    @Test
    fun testLoadAllJobApplicationsUseCase_ForwardsForceRefreshParam() = runTest {
        val testApps = listOf(Factory.createJobApplication(id = "app-repo", company = "Meta"))
        val repo = FakeJobApplicationsListRepository(testApps)
        val useCase = LoadAllJobApplicationsUseCaseImpl(repo)

        val result = useCase(LoadAllJobApplicationsUseCase.Params(forceRefresh = true))
        assertEquals(1, result.size)
        assertEquals(true, repo.lastForceRefresh)
    }
}
