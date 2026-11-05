package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.Confidence
import dev.fnc314.tools.jobtracker.core.data.model.EventActor
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.RecordEventUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateApplicationStatusUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateFitScoreUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetailsUseCasesTest {

    private class FakeJobApplicationRepository : JobApplicationRepository {
        var updatedFitScore: Pair<String, Int>? = null
        var updatedStatusCall: StatusCallParams? = null
        var lastRecordedEvent: TrackerEvent? = null

        data class StatusCallParams(
            val id: String,
            val status: TrackerStatus,
            val date: String?,
            val note: String?,
        )

        override suspend fun getAllJobApplications(): List<JobApplication> = emptyList()
        override suspend fun getJobApplication(id: String): JobApplication? = null
        override suspend fun getCompanies(): List<Company> = emptyList()
        override suspend fun saveJobApplication(app: JobApplication): String = app.id

        override suspend fun updateStatus(
            id: String,
            status: TrackerStatus,
            date: String?,
            note: String?,
        ): JobApplication {
            updatedStatusCall = StatusCallParams(id, status, date, note)
            return Factory.createJobApplication(
                id = id,
                company = "Test Corp",
                status = status,
                notesRendered = note,
            )
        }

        override suspend fun recordEvent(event: TrackerEvent): String {
            lastRecordedEvent = event
            return event.id
        }

        override suspend fun updateFitScore(id: String, score: Int): JobApplication {
            updatedFitScore = id to score
            return Factory.createJobApplication(id = id, company = "Test Corp", fitRating = score)
        }
    }

    // --- UpdateApplicationStatusUseCase Tests (5 Tests) ---

    @Test
    fun testUpdateStatus_StandardTransition() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateApplicationStatusUseCaseImpl(repo)

        val result = useCase(
            UpdateApplicationStatusUseCase.Params(
                id = "app-standard",
                status = TrackerStatus.INTERVIEW,
            )
        )
        assertEquals("app-standard", repo.updatedStatusCall?.id)
        assertEquals(TrackerStatus.INTERVIEW, repo.updatedStatusCall?.status)
        assertNull(repo.updatedStatusCall?.date)
        assertNull(repo.updatedStatusCall?.note)
        assertEquals(TrackerStatus.INTERVIEW, result.status)
    }

    @Test
    fun testUpdateStatus_WithDateAndAuditNote() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateApplicationStatusUseCaseImpl(repo)

        val result = useCase(
            UpdateApplicationStatusUseCase.Params(
                id = "app-audit",
                status = TrackerStatus.INTERVIEW,
                date = "2026-11-04",
                note = "Technical Screen passed: Strong Android architecture performance",
            )
        )
        assertEquals("app-audit", repo.updatedStatusCall?.id)
        assertEquals("2026-11-04", repo.updatedStatusCall?.date)
        assertEquals("Technical Screen passed: Strong Android architecture performance", repo.updatedStatusCall?.note)
        assertEquals(TrackerStatus.INTERVIEW, result.status)
        assertEquals("Technical Screen passed: Strong Android architecture performance", result.notesRendered)
    }

    @Test
    fun testUpdateStatus_BlankNoteAndNullDate() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateApplicationStatusUseCaseImpl(repo)

        val result = useCase(
            UpdateApplicationStatusUseCase.Params(
                id = "app-blank-note",
                status = TrackerStatus.APPLIED,
                date = null,
                note = "",
            )
        )
        assertEquals("", repo.updatedStatusCall?.note)
        assertNull(repo.updatedStatusCall?.date)
        assertEquals(TrackerStatus.APPLIED, result.status)
    }

    @Test
    fun testUpdateStatus_IdempotentStatusWithNewNote() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateApplicationStatusUseCaseImpl(repo)

        val result = useCase(
            UpdateApplicationStatusUseCase.Params(
                id = "app-same-status",
                status = TrackerStatus.APPLIED,
                note = "Re-confirmed submission via company portal",
            )
        )
        assertEquals(TrackerStatus.APPLIED, repo.updatedStatusCall?.status)
        assertEquals("Re-confirmed submission via company portal", repo.updatedStatusCall?.note)
        assertEquals(TrackerStatus.APPLIED, result.status)
    }

    @Test
    fun testUpdateStatus_TerminalStatus() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateApplicationStatusUseCaseImpl(repo)

        val result = useCase(
            UpdateApplicationStatusUseCase.Params(
                id = "app-terminal",
                status = TrackerStatus.REJECTED,
                note = "Position filled internally",
            )
        )
        assertEquals(TrackerStatus.REJECTED, repo.updatedStatusCall?.status)
        assertEquals(TrackerStatus.REJECTED, result.status)
    }

    // --- UpdateFitScoreUseCase Tests (5 Tests) ---

    @Test
    fun testUpdateFitScore_StandardScore() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateFitScoreUseCaseImpl(repo)

        val result = useCase(UpdateFitScoreUseCase.Params(id = "app-fit-1", score = 85))
        assertEquals("app-fit-1" to 85, repo.updatedFitScore)
        assertEquals(85, result.fitScore)
    }

    @Test
    fun testUpdateFitScore_BoundaryZero() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateFitScoreUseCaseImpl(repo)

        val result = useCase(UpdateFitScoreUseCase.Params(id = "app-fit-zero", score = 0))
        assertEquals("app-fit-zero" to 0, repo.updatedFitScore)
        assertEquals(0, result.fitScore)
    }

    @Test
    fun testUpdateFitScore_BoundaryHundred() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateFitScoreUseCaseImpl(repo)

        val result = useCase(UpdateFitScoreUseCase.Params(id = "app-fit-100", score = 100))
        assertEquals("app-fit-100" to 100, repo.updatedFitScore)
        assertEquals(100, result.fitScore)
    }

    @Test
    fun testUpdateFitScore_NegativeScoreEdgeCase() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateFitScoreUseCaseImpl(repo)

        val result = useCase(UpdateFitScoreUseCase.Params(id = "app-fit-neg", score = -5))
        assertEquals("app-fit-neg" to -5, repo.updatedFitScore)
        assertEquals(-5, result.fitScore)
    }

    @Test
    fun testUpdateFitScore_AboveHundredScoreEdgeCase() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = UpdateFitScoreUseCaseImpl(repo)

        val result = useCase(UpdateFitScoreUseCase.Params(id = "app-fit-150", score = 150))
        assertEquals("app-fit-150" to 150, repo.updatedFitScore)
        assertEquals(150, result.fitScore)
    }

    // --- RecordEventUseCase Tests (5 Tests) ---

    @Test
    fun testRecordEvent_StandardEventReturnsEventId() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = RecordEventUseCaseImpl(repo)
        val event = Factory.createTrackerEvent(
            id = "evt-standard",
            applicationId = "app-1",
            type = TrackerEventType.APPLY,
        )

        val resultId = useCase(RecordEventUseCase.Params(event))
        assertEquals("evt-standard", resultId)
        assertEquals("evt-standard", repo.lastRecordedEvent?.id)
        assertEquals(TrackerEventType.APPLY, repo.lastRecordedEvent?.type)
    }

    @Test
    fun testRecordEvent_WithEmailMessageId() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = RecordEventUseCaseImpl(repo)
        val event = Factory.createTrackerEvent(
            id = "evt-email",
            applicationId = "app-1",
            type = TrackerEventType.ACKNOWLEDGE,
            emailMessageId = "msg_18b2c8a9f0e1d2c3",
        )

        val resultId = useCase(RecordEventUseCase.Params(event))
        assertEquals("evt-email", resultId)
        assertEquals("msg_18b2c8a9f0e1d2c3", repo.lastRecordedEvent?.emailMessageId)
    }

    @Test
    fun testRecordEvent_StatusTransitionAudit() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = RecordEventUseCaseImpl(repo)
        val event = Factory.createTrackerEvent(
            id = "evt-transition",
            applicationId = "app-1",
            type = TrackerEventType.CORRECT_STATUS,
            statusFrom = TrackerStatus.APPLIED,
            statusTo = TrackerStatus.INTERVIEW,
            note = "Recruiter reached out on LinkedIn",
        )

        useCase(RecordEventUseCase.Params(event))
        assertEquals(TrackerStatus.APPLIED, repo.lastRecordedEvent?.statusFrom)
        assertEquals(TrackerStatus.INTERVIEW, repo.lastRecordedEvent?.statusTo)
        assertEquals("Recruiter reached out on LinkedIn", repo.lastRecordedEvent?.note)
    }

    @Test
    fun testRecordEvent_NullOptionalFields() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = RecordEventUseCaseImpl(repo)
        val event = Factory.createTrackerEvent(
            id = "evt-nulls",
            applicationId = "app-1",
            note = null,
            emailMessageId = null,
            statusFrom = null,
            statusTo = null,
        )

        useCase(RecordEventUseCase.Params(event))
        assertNull(repo.lastRecordedEvent?.note)
        assertNull(repo.lastRecordedEvent?.emailMessageId)
        assertNull(repo.lastRecordedEvent?.statusFrom)
        assertNull(repo.lastRecordedEvent?.statusTo)
    }

    @Test
    fun testRecordEvent_CustomActorAndConfidence() = runTest {
        val repo = FakeJobApplicationRepository()
        val useCase = RecordEventUseCaseImpl(repo)
        val event = Factory.createTrackerEvent(
            id = "evt-actor",
            applicationId = "app-1",
            actor = EventActor.AGENT,
            confidence = Confidence.LOW,
        )

        useCase(RecordEventUseCase.Params(event))
        assertEquals(EventActor.AGENT, repo.lastRecordedEvent?.actor)
        assertEquals(Confidence.LOW, repo.lastRecordedEvent?.confidence)
    }
}
