package dev.fnc314.tools.jobtracker.features.addjobapplication.data

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.Compensation
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.Location
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.repository.AddJobApplicationRepository
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateLeadUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AddJobApplicationUseCasesTest {

    private class FakeAddJobApplicationRepository : AddJobApplicationRepository {
        var savedApp: JobApplication? = null

        override suspend fun saveJobApplication(application: JobApplication): String {
            savedApp = application
            return application.id
        }

        override suspend fun recordEvent(event: TrackerEvent): String = event.id
    }

    // --- CreateJobApplicationUseCase Tests (5 Tests) ---

    @Test
    fun testCreateApplication_StandardWithGeneratedId() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateJobApplicationUseCaseImpl(repo)
        val app = Factory.createJobApplication(id = "new-app-1", company = "Acme Corp")

        val resultId = useCase(CreateJobApplicationUseCase.Params(app))
        assertEquals("new-app-1", resultId)
        assertEquals("Acme Corp", repo.savedApp?.company)
        assertEquals(TrackerStatus.DRAFTING, repo.savedApp?.status)
    }

    @Test
    fun testCreateApplication_MinimalFields() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateJobApplicationUseCaseImpl(repo)
        val app = Factory.createJobApplication(
            id = "min-1",
            company = "Barebones Inc",
            role = null,
            location = null,
            description = null,
            compensation = null,
        )

        val resultId = useCase(CreateJobApplicationUseCase.Params(app))
        assertEquals("min-1", resultId)
        assertNull(repo.savedApp?.role)
        assertNull(repo.savedApp?.compensation)
        assertNull(repo.savedApp?.location)
    }

    @Test
    fun testCreateApplication_RichMetadata() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateJobApplicationUseCaseImpl(repo)
        val app = Factory.createJobApplication(
            id = "rich-1",
            company = "Stripe",
            role = "Staff Android Engineer",
            location = "Seattle, WA",
            compensation = Compensation(
                min = 220000.0,
                max = 270000.0,
                currency = "USD",
                period = "YEARLY",
                bandSource = "benchmark",
                vsTarget = "AT_TARGET",
            ),
            locationDetail = Location(
                city = "Seattle",
                region = "WA",
                country = "USA",
                workMode = WorkMode.REMOTE,
                relocation = false,
                superCommuter = false,
            ),
        )

        useCase(CreateJobApplicationUseCase.Params(app))
        assertEquals("Staff Android Engineer", repo.savedApp?.role)
        assertEquals(220000.0, repo.savedApp?.compensation?.min)
        assertEquals(270000.0, repo.savedApp?.compensation?.max)
        assertEquals("USD", repo.savedApp?.compensation?.currency)
        assertEquals("Seattle", repo.savedApp?.locationDetail?.city)
    }

    @Test
    fun testCreateApplication_NonDraftingInitialStatus() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateJobApplicationUseCaseImpl(repo)
        val app = Factory.createJobApplication(
            id = "applied-1",
            company = "Direct Apply Co",
            status = TrackerStatus.APPLIED,
            channel = ApplicationChannel.REFERRAL,
        )

        useCase(CreateJobApplicationUseCase.Params(app))
        assertEquals(TrackerStatus.APPLIED, repo.savedApp?.status)
        assertEquals(ApplicationChannel.REFERRAL, repo.savedApp?.channel)
    }

    @Test
    fun testCreateApplication_CustomJobUrlAndDescription() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateJobApplicationUseCaseImpl(repo)
        val app = Factory.createJobApplication(
            id = "desc-1",
            company = "Detailed Co",
            jobUrl = "https://careers.detailed.com/jobs/123",
            description = "Principal Android role leading Compose migrations",
        )

        useCase(CreateJobApplicationUseCase.Params(app))
        assertEquals("https://careers.detailed.com/jobs/123", repo.savedApp?.jobUrl)
        assertEquals("Principal Android role leading Compose migrations", repo.savedApp?.description)
    }

    // --- CreateLeadUseCase Tests (5 Tests) ---

    @Test
    fun testCreateLead_DefaultStatusIsDrafting() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateLeadUseCaseImpl(repo)

        val resultId = useCase(
            CreateLeadUseCase.Params(
                company = "Prospect Labs",
                contact = "Sarah Connor",
                channel = "linkedin",
                notes = listOf("Initial outreach email sent"),
                tags = listOf("lead"),
            )
        )
        assertNotNull(resultId)
        assertEquals("Prospect Labs", repo.savedApp?.company)
        assertEquals(TrackerStatus.DRAFTING, repo.savedApp?.status)
    }

    @Test
    fun testCreateLead_ChannelNormalization() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateLeadUseCaseImpl(repo)

        useCase(
            CreateLeadUseCase.Params(
                company = "LinkedIn Source",
                channel = "linkedin",
            )
        )
        assertEquals(ApplicationChannel.LINKEDIN, repo.savedApp?.channel)
    }

    @Test
    fun testCreateLead_UnknownChannelFallback() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateLeadUseCaseImpl(repo)

        useCase(
            CreateLeadUseCase.Params(
                company = "Obscure Portal Co",
                channel = "totally_unsupported_channel_xyz",
            )
        )
        // Unknown raw channel falls back to OTHER
        assertEquals(ApplicationChannel.OTHER, repo.savedApp?.channel)
    }

    @Test
    fun testCreateLead_NotesJoinedWithNewlines() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateLeadUseCaseImpl(repo)

        useCase(
            CreateLeadUseCase.Params(
                company = "Multi Note Co",
                notes = listOf("Note 1: Initial call", "Note 2: Sent CV", "Note 3: Waiting on feedback"),
            )
        )
        assertEquals("Note 1: Initial call\nNote 2: Sent CV\nNote 3: Waiting on feedback", repo.savedApp?.notesRendered)
    }

    @Test
    fun testCreateLead_EmptyNotes() = runTest {
        val repo = FakeAddJobApplicationRepository()
        val useCase = CreateLeadUseCaseImpl(repo)

        useCase(
            CreateLeadUseCase.Params(
                company = "Empty Notes Co",
                notes = emptyList(),
            )
        )
        assertEquals("", repo.savedApp?.notesRendered)
    }
}
