package dev.fnc314.tools.jobtracker.features.trackeredits.data

import dev.fnc314.tools.jobtracker.core.data.model.Confidence
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEditKind
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEditState
import dev.fnc314.tools.jobtracker.core.data.model.Trigger
import dev.fnc314.tools.jobtracker.core.data.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackerEditsUseCasesTest {

    private fun createTestEdit(
        id: String = "edit-1",
        applicationId: String = "app-1",
        kind: TrackerEditKind = TrackerEditKind.STATUS,
        state: TrackerEditState = TrackerEditState.PENDING,
    ) = TrackerEdit(
        id = id,
        applicationId = applicationId,
        kind = kind,
        state = state,
        trigger = Trigger.EMAIL_SYNC,
        changes = emptyList(),
        unmatched = emptyList(),
        sourceEmail = null,
        scope = null,
        confidence = Confidence.HIGH,
        createdAt = null,
        expiresAt = null,
    )

    private class FakeTrackerEditsRepository(
        private val helper: TrackerEditsUseCasesTest,
    ) : TrackerEditsRepository {
        var approvedId: String? = null
        var rejectedId: String? = null
        var expiredCount: Int = 3

        override suspend fun getPendingEdits(): List<TrackerEdit> = listOf(
            helper.createTestEdit(
                id = "edit-1",
                applicationId = "app-1",
                kind = TrackerEditKind.STATUS,
                state = TrackerEditState.PENDING,
            )
        )

        override suspend fun approveEdit(id: String, note: String?): TrackerEdit {
            approvedId = id
            return helper.createTestEdit(id = id, state = TrackerEditState.APPROVED)
        }

        override suspend fun rejectEdit(id: String, reason: String): TrackerEdit {
            rejectedId = id
            return helper.createTestEdit(id = id, state = TrackerEditState.REJECTED)
        }

        override suspend fun expireStaleEdits(): Int = expiredCount
    }

    @Test
    fun testLoadPendingEdits() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = LoadPendingEditsUseCaseImpl(repo)
        val result = useCase(LoadPendingEditsUseCase.Params())
        assertEquals(1, result.size)
        assertEquals("edit-1", result.first().id)
    }

    @Test
    fun testApproveEdit() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = ApproveEditUseCaseImpl(repo)
        val result = useCase(ApproveEditUseCase.Params(id = "edit-99", note = "Approved"))
        assertEquals("edit-99", repo.approvedId)
        assertEquals(TrackerEditState.APPROVED, result.state)
    }

    @Test
    fun testRejectEdit() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = RejectEditUseCaseImpl(repo)
        val result = useCase(RejectEditUseCase.Params(id = "edit-99", reason = "Duplicate"))
        assertEquals("edit-99", repo.rejectedId)
        assertEquals(TrackerEditState.REJECTED, result.state)
    }

    @Test
    fun testExpireStaleEdits() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = ExpireStaleEditsUseCaseImpl(repo)
        val result = useCase(ExpireStaleEditsUseCase.Params())
        assertEquals(3, result)
    }
}
