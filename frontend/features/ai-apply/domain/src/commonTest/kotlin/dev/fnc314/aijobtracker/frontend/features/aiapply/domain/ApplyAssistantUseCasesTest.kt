package dev.fnc314.aijobtracker.frontend.features.aiapply.domain

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyArtifactJobState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyEvaluationResult
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplySessionState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyWorkflowStep
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.repository.ApplyAssistantRepository
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ConfirmApplicationSubmissionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.EvaluateJobPostingUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.GenerateApplicationArtifactsUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.SyncDraftedArtifactsUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.ConfirmApplicationSubmissionUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.EvaluateJobPostingUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.GenerateApplicationArtifactsUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.RefineJobEvaluationWithAiUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.SyncDraftedArtifactsUseCaseImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ApplyAssistantUseCasesTest {
    private class FakeApplyAssistantRepository : ApplyAssistantRepository {
        private val flow = MutableStateFlow<ApplySessionState?>(null)

        override fun observeLatestSession(): Flow<ApplySessionState?> = flow

        override suspend fun evaluateDeterministic(
            url: String?,
            text: String?,
            company: String?,
            role: String?,
        ): ApplySessionState {
            val session = ApplySessionState(
                id = "sess-1",
                postingUrl = url.orEmpty(),
                postingText = text.orEmpty(),
                company = company ?: "Walmart",
                role = role ?: "Principal Android Engineer",
                evaluation = ApplyEvaluationResult(
                    applicationId = "walmart-principal-2026-11-18",
                    company = company ?: "Walmart",
                    role = role ?: "Principal Android Engineer",
                    score = 78,
                    deterministicScore = 78,
                    verdict = "GOOD_FIT",
                    status = TrackerStatus.RANKED,
                ),
                currentStep = ApplyWorkflowStep.AWAITING_GENERATE_CONFIRMATION,
            )
            flow.value = session
            return session
        }

        override suspend fun refineWithAi(sessionId: String): ApplySessionState {
            val current = flow.value!!
            val updated = current.copy(
                evaluation = current.evaluation!!.copy(
                    score = 91,
                    aiScore = 91,
                    isAiEvaluated = true,
                    aiRationale = "Strong Android platform match",
                )
            )
            flow.value = updated
            return updated
        }

        override suspend fun respondToGenerateAssets(sessionId: String, generate: Boolean): ApplySessionState {
            val current = flow.value!!
            val updated = if (generate) {
                current.copy(
                    evaluation = current.evaluation!!.copy(status = TrackerStatus.DRAFTING),
                    artifactJob = ApplyArtifactJobState("job_1", "walmart-principal-2026-11-18", "IN_PROGRESS"),
                    currentStep = ApplyWorkflowStep.DRAFTING,
                )
            } else {
                current.copy(currentStep = ApplyWorkflowStep.AWAITING_INPUT)
            }
            flow.value = updated
            return updated
        }

        override fun observeArtifactJob(jobId: String): Flow<ApplyArtifactJobState?> =
            flowOf(ApplyArtifactJobState(jobId, "walmart-principal-2026-11-18", "SUCCESS", "cv.pdf", "cover.pdf"))

        override suspend fun applyArtifactJobUpdate(
            sessionId: String,
            jobState: ApplyArtifactJobState,
        ): ApplySessionState {
            val current = flow.value!!
            val updated = current.copy(
                evaluation = current.evaluation!!.copy(status = TrackerStatus.DRAFTED),
                artifactJob = jobState,
                currentStep = ApplyWorkflowStep.AWAITING_SUBMISSION_CONFIRMATION,
            )
            flow.value = updated
            return updated
        }

        override suspend fun confirmSubmission(sessionId: String, submitted: Boolean): ApplySessionState {
            val current = flow.value!!
            val nextStatus = if (submitted) TrackerStatus.SUBMITTED else TrackerStatus.DRAFTED
            val updated = current.copy(
                evaluation = current.evaluation!!.copy(status = nextStatus),
                currentStep = if (submitted) ApplyWorkflowStep.SUBMITTED else ApplyWorkflowStep.DRAFTED,
            )
            flow.value = updated
            return updated
        }

        override suspend fun resetSession() {
            flow.value = null
        }
    }

    @Test
    fun `full funnel progresses RANKED to DRAFTING to DRAFTED to SUBMITTED`() = runTest {
        val repo = FakeApplyAssistantRepository()
        val evaluate = EvaluateJobPostingUseCaseImpl(repo)
        val refine = RefineJobEvaluationWithAiUseCaseImpl(repo)
        val generate = GenerateApplicationArtifactsUseCaseImpl(repo)
        val syncDrafted = SyncDraftedArtifactsUseCaseImpl(repo)
        val submit = ConfirmApplicationSubmissionUseCaseImpl(repo)

        val ranked = evaluate(EvaluateJobPostingUseCase.Params(text = "Kotlin Compose", company = "Walmart"))
        assertEquals(TrackerStatus.RANKED, ranked.evaluation?.status)
        assertEquals(78, ranked.evaluation?.score)

        val refined = refine(ranked.id)
        assertTrue(refined.evaluation?.isAiEvaluated == true)
        assertEquals(91, refined.evaluation?.score)

        val drafting = generate(GenerateApplicationArtifactsUseCase.Params(ranked.id, generate = true))
        assertEquals(TrackerStatus.DRAFTING, drafting.evaluation?.status)

        val drafted = syncDrafted(
            SyncDraftedArtifactsUseCase.Params(
                ranked.id,
                ApplyArtifactJobState("job_1", "walmart-principal-2026-11-18", "SUCCESS", "cv.pdf", "cover.pdf"),
            )
        )
        assertEquals(TrackerStatus.DRAFTED, drafted.evaluation?.status)

        val submitted = submit(ConfirmApplicationSubmissionUseCase.Params(ranked.id, submitted = true))
        assertEquals(TrackerStatus.SUBMITTED, submitted.evaluation?.status)
    }

    @Test
    fun `declining asset generation keeps status RANKED`() = runTest {
        val repo = FakeApplyAssistantRepository()
        val evaluate = EvaluateJobPostingUseCaseImpl(repo)
        val generate = GenerateApplicationArtifactsUseCaseImpl(repo)

        val ranked = evaluate(EvaluateJobPostingUseCase.Params(url = "https://jobs.example.com/1"))
        val declined = generate(GenerateApplicationArtifactsUseCase.Params(ranked.id, generate = false))
        assertEquals(TrackerStatus.RANKED, declined.evaluation?.status)
        assertEquals(ApplyWorkflowStep.AWAITING_INPUT, declined.currentStep)
    }
}
