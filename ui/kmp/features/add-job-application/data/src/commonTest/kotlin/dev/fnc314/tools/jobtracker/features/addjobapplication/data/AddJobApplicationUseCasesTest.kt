package dev.fnc314.tools.jobtracker.features.addjobapplication.data

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateLeadUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AddJobApplicationUseCasesTest {

    private class FakeJobApplicationRepository : JobApplicationRepository {
        var savedApp: JobApplication? = null

        override suspend fun getAllJobApplications(): List<JobApplication> = emptyList()
        override suspend fun getJobApplication(id: String): JobApplication? = null
        override suspend fun getCompanies(): List<Company> = emptyList()
        override suspend fun saveJobApplication(app: JobApplication): String {
            savedApp = app
            return app.id
        }
        override suspend fun updateStatus(id: String, status: TrackerStatus, date: String?, note: String?): JobApplication =
            savedApp ?: Factory.createJobApplication(id = id, company = "Test")
        override suspend fun recordEvent(event: TrackerEvent): String = event.id
        override suspend fun updateFitScore(id: String, score: Int): JobApplication =
            savedApp ?: Factory.createJobApplication(id = id, company = "Test")
    }

    @Test
    fun testCreateJobApplicationUseCase() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = CreateJobApplicationUseCaseImpl(repo)
        val app = Factory.createJobApplication(id = "new-app-1", company = "Acme Corp")

        val resultId = useCase(CreateJobApplicationUseCase.Params(app))
        assertEquals("new-app-1", resultId)
        assertEquals("Acme Corp", repo.savedApp?.company)
    }

    @Test
    fun testCreateLeadUseCase() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = CreateLeadUseCaseImpl(repo)

        val resultId = useCase(
            CreateLeadUseCase.Params(
                company = "Lead Co",
                contact = "Jane Doe",
                channel = "linkedin",
                notes = listOf("Met at tech conference"),
                tags = listOf("lead", "fintech"),
            )
        )
        assertEquals("Lead Co", repo.savedApp?.company)
        assertEquals(TrackerStatus.DRAFTING, repo.savedApp?.status)
    }
}
