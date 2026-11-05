package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateApplicationStatusUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateFitScoreUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DetailsUseCasesTest {

    private class FakeJobApplicationRepository : JobApplicationRepository {
        var updatedFitScore: Pair<String, Int>? = null
        var updatedStatus: Pair<String, TrackerStatus>? = null

        override suspend fun getAllJobApplications(): List<JobApplication> = emptyList()
        override suspend fun getJobApplication(id: String): JobApplication? = null
        override suspend fun getCompanies(): List<Company> = emptyList()
        override suspend fun saveJobApplication(app: JobApplication): String = app.id
        override suspend fun updateStatus(
            id: String,
            status: TrackerStatus,
            date: String?,
            note: String?
        ): JobApplication {
            updatedStatus = id to status
            return Factory.createJobApplication(id = id, company = "Test Corp", status = status)
        }
        override suspend fun recordEvent(event: TrackerEvent): String = event.id
        override suspend fun updateFitScore(id: String, score: Int): JobApplication {
            updatedFitScore = id to score
            return Factory.createJobApplication(id = id, company = "Test Corp", fitRating = score)
        }
    }

    @Test
    fun testUpdateFitScoreUseCase() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateFitScoreUseCaseImpl(repo)

        val result = useCase(UpdateFitScoreUseCase.Params(id = "app-123", score = 94))
        assertEquals("app-123", repo.updatedFitScore?.first)
        assertEquals(94, repo.updatedFitScore?.second)
        assertEquals(94, result.fitScore)
    }

    @Test
    fun testUpdateApplicationStatusUseCase() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateApplicationStatusUseCaseImpl(repo)

        val result = useCase(
            UpdateApplicationStatusUseCase.Params(
                id = "app-456",
                status = TrackerStatus.INTERVIEW,
                date = "2026-11-04",
                note = "Technical Screen passed"
            )
        )
        assertEquals("app-456", repo.updatedStatus?.first)
        assertEquals(TrackerStatus.INTERVIEW, repo.updatedStatus?.second)
        assertEquals(TrackerStatus.INTERVIEW, result.status)
    }
}
