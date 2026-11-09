package dev.fnc314.tools.jobtracker.features.trackeredits.domain

import dev.fnc314.tools.jobtracker.core.data.model.Confidence
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEditKind
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEditState
import dev.fnc314.tools.jobtracker.core.data.model.Trigger
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ApproveEditUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ExpireStaleEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.LoadPendingEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.RejectEditUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.impl.ApproveEditUseCaseImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.impl.ExpireStaleEditsUseCaseImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.impl.LoadPendingEditsUseCaseImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.impl.RejectEditUseCaseImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackerEditsUseCasesTest {

    private fun createTestEdit(
        id: String = "edit-1",
        applicationId: String = "app-1",
        kind: TrackerEditKind = TrackerEditKind.STATUS,
        state: TrackerEditState = TrackerEditState.PENDING,
        confidence: Confidence = Confidence.HIGH,
    ) = TrackerEdit(
        id = id,
        applicationId = applicationId,
        kind = kind,
        state = state,
        trigger = Trigger.APPLICATION_CREATED,
        changes = emptyList(),
        unmatched = emptyList(),
        sourceEmail = null,
        scope = null,
        confidence = confidence,
        createdAt = null,
        expiresAt = null,
    )

    private class FakeTrackerEditsRepository(
        private val helper: TrackerEditsUseCasesTest,
    ) : TrackerEditsRepository {
        var approvedId: String? = null
        var approvedNote: String? = null
        var rejectedId: String? = null
        var rejectedReason: String? = null
        var expiredCount: Int = 3
        var pendingEdits: List<TrackerEdit> = listOf(
            helper.createTestEdit(
                id = "edit-1",
                applicationId = "app-1",
                kind = TrackerEditKind.STATUS,
                state = TrackerEditState.PENDING,
            )
        )

        override suspend fun getPendingEdits(): List<TrackerEdit> = pendingEdits

        override suspend fun approveEdit(id: String, note: String?): TrackerEdit {
            approvedId = id
            approvedNote = note
            return helper.createTestEdit(id = id, state = TrackerEditState.APPROVED)
        }

        override suspend fun rejectEdit(id: String, reason: String): TrackerEdit {
            rejectedId = id
            rejectedReason = reason
            return helper.createTestEdit(id = id, state = TrackerEditState.REJECTED)
        }

        override suspend fun expireStaleEdits(): Int = expiredCount
    }

    // --- LoadPendingEditsUseCase & ExpireStaleEditsUseCase Tests (5 Tests) ---

    @Test
    fun testLoadPendingEdits_ReturnsOnlyPending() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = LoadPendingEditsUseCaseImpl(repo)
        val result = useCase(LoadPendingEditsUseCase.Params())
        assertEquals(1, result.size)
        assertEquals("edit-1", result.first().id)
        assertEquals(TrackerEditState.PENDING, result.first().state)
    }

    @Test
    fun testLoadPendingEdits_EmptyRepositoryReturnsEmptyList() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        repo.pendingEdits = emptyList()
        val useCase = LoadPendingEditsUseCaseImpl(repo)
        val result = useCase(LoadPendingEditsUseCase.Params())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testLoadPendingEdits_ForceRefreshFlag() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = LoadPendingEditsUseCaseImpl(repo)
        val result = useCase(LoadPendingEditsUseCase.Params(forceRefresh = true))
        assertEquals(1, result.size)
    }

    @Test
    fun testExpireStaleEdits_MultipleStaleReturnsCount() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        repo.expiredCount = 5
        val useCase = ExpireStaleEditsUseCaseImpl(repo)
        val result = useCase(ExpireStaleEditsUseCase.Params())
        assertEquals(5, result)
    }

    @Test
    fun testExpireStaleEdits_ZeroStaleReturnsZero() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        repo.expiredCount = 0
        val useCase = ExpireStaleEditsUseCaseImpl(repo)
        val result = useCase(ExpireStaleEditsUseCase.Params())
        assertEquals(0, result)
    }

    // --- ApproveEditUseCase & RejectEditUseCase Tests (5 Tests) ---

    @Test
    fun testApproveEdit_WithNote() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = ApproveEditUseCaseImpl(repo)
        val result = useCase(ApproveEditUseCase.Params(id = "edit-10", note = "Approved via email parser verification"))
        assertEquals("edit-10", repo.approvedId)
        assertEquals("Approved via email parser verification", repo.approvedNote)
        assertEquals(TrackerEditState.APPROVED, result.state)
    }

    @Test
    fun testApproveEdit_NullNote() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = ApproveEditUseCaseImpl(repo)
        val result = useCase(ApproveEditUseCase.Params(id = "edit-null-note", note = null))
        assertEquals("edit-null-note", repo.approvedId)
        assertNull(repo.approvedNote)
        assertEquals(TrackerEditState.APPROVED, result.state)
    }

    @Test
    fun testRejectEdit_WithReason() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = RejectEditUseCaseImpl(repo)
        val result = useCase(RejectEditUseCase.Params(id = "edit-20", reason = "Duplicate calendar invite"))
        assertEquals("edit-20", repo.rejectedId)
        assertEquals("Duplicate calendar invite", repo.rejectedReason)
        assertEquals(TrackerEditState.REJECTED, result.state)
    }

    @Test
    fun testRejectEdit_EmptyReason() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val useCase = RejectEditUseCaseImpl(repo)
        val result = useCase(RejectEditUseCase.Params(id = "edit-empty-reason", reason = ""))
        assertEquals("edit-empty-reason", repo.rejectedId)
        assertEquals("", repo.rejectedReason)
        assertEquals(TrackerEditState.REJECTED, result.state)
    }

    @Test
    fun testApproveAndReject_PreservesMetadata() = runTest {
        val repo = FakeTrackerEditsRepository(this@TrackerEditsUseCasesTest)
        val approveUseCase = ApproveEditUseCaseImpl(repo)
        val rejectUseCase = RejectEditUseCaseImpl(repo)

        val approved = approveUseCase(ApproveEditUseCase.Params(id = "meta-1"))
        val rejected = rejectUseCase(RejectEditUseCase.Params(id = "meta-2", reason = "Invalid status"))

        assertEquals(TrackerEditKind.STATUS, approved.kind)
        assertEquals(TrackerEditKind.STATUS, rejected.kind)
        assertEquals("app-1", approved.applicationId)
        assertEquals("app-1", rejected.applicationId)
    }
}
