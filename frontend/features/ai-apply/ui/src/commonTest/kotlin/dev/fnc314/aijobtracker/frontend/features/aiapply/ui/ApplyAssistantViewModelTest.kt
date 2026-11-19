package dev.fnc314.aijobtracker.frontend.features.aiapply.ui

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyArtifactJobState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyEvaluationResult
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyInputMode
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplySessionState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyWorkflowStep
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ConfirmApplicationSubmissionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.EvaluateJobPostingUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.GenerateApplicationArtifactsUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ObserveApplySessionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ObserveArtifactJobUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.RefineJobEvaluationWithAiUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ResetApplySessionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.SyncDraftedArtifactsUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ApplyAssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sessionFlow = MutableStateFlow<ApplySessionState?>(null)
    private val artifactJobFlow = MutableStateFlow<ApplyArtifactJobState?>(null)
    private var lastEvaluateParams: EvaluateJobPostingUseCase.Params? = null
    private var aiDeferred: CompletableDeferred<ApplySessionState>? = null
    private var artifactObserverError: Throwable? = null

    @BeforeTest
    fun setUp(): Unit {
        Dispatchers.setMain(testDispatcher)
        sessionFlow.value = null
        artifactJobFlow.value = null
        lastEvaluateParams = null
        aiDeferred = null
        artifactObserverError = null
    }

    @AfterTest
    fun tearDown(): Unit {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ApplyAssistantViewModel {
        val observeSession = ObserveApplySessionUseCase { sessionFlow }
        val evaluate = EvaluateJobPostingUseCase { params ->
            lastEvaluateParams = params
            val initialSession = ApplySessionState(
                id = "sess-1",
                inputMode = if (params.url != null) ApplyInputMode.URL else ApplyInputMode.DESCRIPTION,
                postingUrl = params.url.orEmpty(),
                postingText = params.text.orEmpty(),
                company = params.company ?: "Acme FinTech",
                role = params.role ?: "Principal Android Engineer",
                evaluation = ApplyEvaluationResult(
                    applicationId = "acme-fintech-principal-android-engineer",
                    company = params.company ?: "Acme FinTech",
                    role = params.role ?: "Principal Android Engineer",
                    score = 76,
                    deterministicScore = 76,
                    aiScore = null,
                    isAiEvaluated = false,
                    verdict = "GOOD_FIT",
                    status = TrackerStatus.RANKED,
                ),
                currentStep = ApplyWorkflowStep.AWAITING_GENERATE_CONFIRMATION,
            )
            sessionFlow.value = initialSession
            initialSession
        }
        val refineWithAi = RefineJobEvaluationWithAiUseCase { _ ->
            val pending = aiDeferred
            if (pending != null) {
                val refined = pending.await()
                sessionFlow.value = refined
                refined
            } else {
                val current = requireNotNull(sessionFlow.value)
                val updated = current.copy(
                    evaluation = current.evaluation?.copy(
                        score = 88,
                        aiScore = 88,
                        isAiEvaluated = true,
                        verdict = "STRONG_MATCH",
                    ),
                )
                sessionFlow.value = updated
                updated
            }
        }
        val generateArtifacts = GenerateApplicationArtifactsUseCase { params ->
            val current = requireNotNull(sessionFlow.value)
            val eval = requireNotNull(current.evaluation)
            val updated = if (params.generate) {
                val job = ApplyArtifactJobState(
                    jobId = "job-123",
                    applicationId = eval.applicationId,
                    status = "IN_PROGRESS",
                )
                current.copy(
                    evaluation = eval.copy(status = TrackerStatus.DRAFTING),
                    artifactJob = job,
                    currentStep = ApplyWorkflowStep.DRAFTING,
                )
            } else {
                current.copy(
                    evaluation = eval.copy(status = TrackerStatus.RANKED),
                    currentStep = ApplyWorkflowStep.AWAITING_INPUT,
                )
            }
            sessionFlow.value = updated
            updated
        }
        val observeArtifactJob = ObserveArtifactJobUseCase { _ ->
            val err = artifactObserverError
            if (err != null) {
                flow { throw err }
            } else {
                artifactJobFlow
            }
        }
        val syncDrafted = SyncDraftedArtifactsUseCase { params ->
            val current = requireNotNull(sessionFlow.value)
            val eval = requireNotNull(current.evaluation)
            val isSuccess = params.jobState.status == "SUCCESS"
            val updated = current.copy(
                evaluation = eval.copy(
                    status = if (isSuccess) TrackerStatus.DRAFTED else eval.status,
                ),
                artifactJob = params.jobState,
                currentStep = if (isSuccess) {
                    ApplyWorkflowStep.AWAITING_SUBMISSION_CONFIRMATION
                } else {
                    current.currentStep
                },
            )
            sessionFlow.value = updated
            updated
        }
        val confirmSubmission = ConfirmApplicationSubmissionUseCase { params ->
            val current = requireNotNull(sessionFlow.value)
            val eval = requireNotNull(current.evaluation)
            val updated = current.copy(
                evaluation = eval.copy(
                    status = if (params.submitted) TrackerStatus.SUBMITTED else TrackerStatus.DRAFTED,
                ),
                currentStep = if (params.submitted) {
                    ApplyWorkflowStep.SUBMITTED
                } else {
                    ApplyWorkflowStep.DRAFTED
                },
            )
            sessionFlow.value = updated
            updated
        }
        val resetSession = ResetApplySessionUseCase {
            sessionFlow.value = null
            artifactJobFlow.value = null
        }
        return ApplyAssistantViewModel(
            observeApplySession = observeSession,
            evaluateJobPosting = evaluate,
            refineJobEvaluationWithAi = refineWithAi,
            generateApplicationArtifacts = generateArtifacts,
            observeArtifactJob = observeArtifactJob,
            syncDraftedArtifacts = syncDrafted,
            confirmApplicationSubmission = confirmSubmission,
            resetApplySession = resetSession,
        )
    }

    @Test
    fun inputModeSwitchAndSubmit_TransitionsDeterministicToAiScore(): Unit = runTest(testDispatcher) {
        val deferred = CompletableDeferred<ApplySessionState>()
        aiDeferred = deferred
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(ApplyInputMode.DESCRIPTION, viewModel.uiState.value.inputMode)

        viewModel.onIntent(ApplyAssistantUiIntent.SelectInputMode(ApplyInputMode.URL))
        viewModel.onIntent(ApplyAssistantUiIntent.ChangeUrl("https://jobs.example.com/123"))
        viewModel.onIntent(ApplyAssistantUiIntent.ChangeCompany("Acme FinTech"))
        viewModel.onIntent(ApplyAssistantUiIntent.ChangeRole("Principal Android Engineer"))
        viewModel.onIntent(ApplyAssistantUiIntent.SubmitEvaluation)
        runCurrent()

        val evaluateParams = assertNotNull(lastEvaluateParams)
        assertEquals("https://jobs.example.com/123", evaluateParams.url)
        assertNull(evaluateParams.text)

        val tier1Eval = assertNotNull(viewModel.uiState.value.session?.evaluation)
        assertEquals(76, tier1Eval.score)
        assertEquals(76, tier1Eval.deterministicScore)
        assertFalse(tier1Eval.isAiEvaluated)
        assertEquals(TrackerStatus.RANKED, tier1Eval.status)
        assertTrue(viewModel.uiState.value.isAiRefining)

        val currentSession = requireNotNull(viewModel.uiState.value.session)
        deferred.complete(
            currentSession.copy(
                evaluation = tier1Eval.copy(
                    score = 89,
                    aiScore = 89,
                    isAiEvaluated = true,
                    verdict = "STRONG_MATCH",
                ),
            ),
        )
        advanceUntilIdle()

        val tier2Eval = assertNotNull(viewModel.uiState.value.session?.evaluation)
        assertEquals(89, tier2Eval.score)
        assertEquals(89, tier2Eval.aiScore)
        assertTrue(tier2Eval.isAiEvaluated)
        assertFalse(viewModel.uiState.value.isAiRefining)
    }

    @Test
    fun descriptionModeBlankInput_SetsValidationErrorWithoutSubmitting(): Unit = runTest(testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onIntent(ApplyAssistantUiIntent.SelectInputMode(ApplyInputMode.DESCRIPTION))
        viewModel.onIntent(ApplyAssistantUiIntent.ChangeDescription("   "))
        viewModel.onIntent(ApplyAssistantUiIntent.SubmitEvaluation)
        runCurrent()

        assertNull(lastEvaluateParams)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun generateAssets_NoKeepsRanked_YesTransitionsDraftingThenDrafted(): Unit = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ApplyAssistantUiIntent.ChangeDescription("Kotlin Jetpack Compose Gradle role"))
        viewModel.onIntent(ApplyAssistantUiIntent.SubmitEvaluation)
        advanceUntilIdle()

        viewModel.onIntent(ApplyAssistantUiIntent.RespondGenerateAssets(generate = false))
        advanceUntilIdle()
        assertEquals(TrackerStatus.RANKED, viewModel.uiState.value.session?.evaluation?.status)
        assertEquals(ApplyWorkflowStep.AWAITING_INPUT, viewModel.uiState.value.session?.currentStep)

        viewModel.onIntent(ApplyAssistantUiIntent.RespondGenerateAssets(generate = true))
        runCurrent()
        assertEquals(TrackerStatus.DRAFTING, viewModel.uiState.value.session?.evaluation?.status)
        assertEquals(ApplyWorkflowStep.DRAFTING, viewModel.uiState.value.session?.currentStep)

        artifactJobFlow.value = ApplyArtifactJobState(
            jobId = "job-123",
            applicationId = "acme-fintech-principal-android-engineer",
            status = "SUCCESS",
            cvPdfUrl = "/tmp/cv.pdf",
            coverLetterPdfUrl = "/tmp/cover.pdf",
        )
        advanceUntilIdle()

        val draftedSession = assertNotNull(viewModel.uiState.value.session)
        assertEquals(TrackerStatus.DRAFTED, draftedSession.evaluation?.status)
        assertEquals(ApplyWorkflowStep.AWAITING_SUBMISSION_CONFIRMATION, draftedSession.currentStep)
        assertEquals("/tmp/cv.pdf", draftedSession.artifactJob?.cvPdfUrl)
    }

    @Test
    fun didYouSubmit_NoKeepsDrafted_YesTransitionsSubmitted(): Unit = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(ApplyAssistantUiIntent.ChangeDescription("Staff Android role"))
        viewModel.onIntent(ApplyAssistantUiIntent.SubmitEvaluation)
        advanceUntilIdle()

        viewModel.onIntent(ApplyAssistantUiIntent.RespondGenerateAssets(generate = true))
        runCurrent()
        artifactJobFlow.value = ApplyArtifactJobState(
            jobId = "job-123",
            applicationId = "acme-fintech-principal-android-engineer",
            status = "SUCCESS",
        )
        advanceUntilIdle()
        assertEquals(TrackerStatus.DRAFTED, viewModel.uiState.value.session?.evaluation?.status)

        viewModel.onIntent(ApplyAssistantUiIntent.RespondSubmitted(submitted = false))
        advanceUntilIdle()
        assertEquals(TrackerStatus.DRAFTED, viewModel.uiState.value.session?.evaluation?.status)
        assertEquals(ApplyWorkflowStep.DRAFTED, viewModel.uiState.value.session?.currentStep)

        viewModel.onIntent(ApplyAssistantUiIntent.RespondSubmitted(submitted = true))
        advanceUntilIdle()
        assertEquals(TrackerStatus.SUBMITTED, viewModel.uiState.value.session?.evaluation?.status)
        assertEquals(ApplyWorkflowStep.SUBMITTED, viewModel.uiState.value.session?.currentStep)
    }

    @Test
    fun artifactObserverError_DoesNotCrash_AndSurfacesErrorMessage(): Unit = runTest(testDispatcher) {
        artifactObserverError = IllegalStateException("PERMISSION_DENIED: Missing or insufficient permissions.")
        val viewModel = createViewModel()
        viewModel.onIntent(ApplyAssistantUiIntent.ChangeDescription("Staff Android role"))
        viewModel.onIntent(ApplyAssistantUiIntent.SubmitEvaluation)
        advanceUntilIdle()

        viewModel.onIntent(ApplyAssistantUiIntent.RespondGenerateAssets(generate = true))
        advanceUntilIdle()

        assertEquals(
            "PERMISSION_DENIED: Missing or insufficient permissions.",
            viewModel.uiState.value.errorMessage,
        )
    }
}
