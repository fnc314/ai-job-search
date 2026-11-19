package dev.fnc314.aijobtracker.frontend.features.aiapply.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable presentation state for the single-view Apply Assistant destination.
 *
 * @property session Active persisted [ApplySessionState], if an evaluation has begun.
 * @property inputMode Currently selected input field mode ([ApplyInputMode.URL] or [ApplyInputMode.DESCRIPTION]).
 * @property urlInput Current URL text value.
 * @property descriptionInput Current job description Markdown/text value.
 * @property companyInput Optional company override value.
 * @property roleInput Optional role override value.
 * @property isEvaluating True while initial deterministic evaluation is executing.
 * @property isAiRefining True while background Tier 2 `/ai/rank` calibration is executing.
 * @property isSubmittingPrompt True while handling Yes/No confirmation transitions.
 * @property errorMessage Transient user-facing error message, if any.
 */
public data class ApplyAssistantUiState(
    public val session: ApplySessionState? = null,
    public val inputMode: ApplyInputMode = ApplyInputMode.DESCRIPTION,
    public val urlInput: String = "",
    public val descriptionInput: String = "",
    public val companyInput: String = "",
    public val roleInput: String = "",
    public val isEvaluating: Boolean = false,
    public val isAiRefining: Boolean = false,
    public val isSubmittingPrompt: Boolean = false,
    public val errorMessage: String? = null,
)

/**
 * User actions dispatched to [ApplyAssistantViewModel].
 */
public sealed interface ApplyAssistantUiIntent {
    /** Switches between URL and Description input fields. */
    public data class SelectInputMode(
        /** Target input mode. */
        public val mode: ApplyInputMode,
    ) : ApplyAssistantUiIntent

    /** Updates posting URL text input. */
    public data class ChangeUrl(
        /** Updated URL text. */
        public val value: String,
    ) : ApplyAssistantUiIntent

    /** Updates posting Markdown description text input. */
    public data class ChangeDescription(
        /** Updated job description text. */
        public val value: String,
    ) : ApplyAssistantUiIntent

    /** Updates optional company override input. */
    public data class ChangeCompany(
        /** Updated company override. */
        public val value: String,
    ) : ApplyAssistantUiIntent

    /** Updates optional role override input. */
    public data class ChangeRole(
        /** Updated role title override. */
        public val value: String,
    ) : ApplyAssistantUiIntent

    /** Submits URL or job description for instant deterministic ranking followed by async AI calibration. */
    public data object SubmitEvaluation : ApplyAssistantUiIntent

    /** Answers the "Generate assets?" prompt (`true` -> DRAFTING -> DRAFTED, `false` -> remains RANKED). */
    public data class RespondGenerateAssets(
        /** True to start artifact drafting, false to keep RANKED. */
        public val generate: Boolean,
    ) : ApplyAssistantUiIntent

    /** Answers the "Did you submit?" prompt (`true` -> SUBMITTED, `false` -> remains DRAFTED). */
    public data class RespondSubmitted(
        /** True if submitted, false to remain DRAFTED. */
        public val submitted: Boolean,
    ) : ApplyAssistantUiIntent

    /** Clears current session state to start a fresh evaluation. */
    public data object ResetSession : ApplyAssistantUiIntent

    /** Dismisses transient error banners. */
    public data object ClearError : ApplyAssistantUiIntent
}

/**
 * ViewModel orchestrating the Apply Assistant workflow, Tier 1 -> Tier 2 animated ranking,
 * Room state recovery, and remote artifact job observation across configuration changes and process death.
 */
public class ApplyAssistantViewModel(
    private val observeApplySession: ObserveApplySessionUseCase,
    private val evaluateJobPosting: EvaluateJobPostingUseCase,
    private val refineJobEvaluationWithAi: RefineJobEvaluationWithAiUseCase,
    private val generateApplicationArtifacts: GenerateApplicationArtifactsUseCase,
    private val observeArtifactJob: ObserveArtifactJobUseCase,
    private val syncDraftedArtifacts: SyncDraftedArtifactsUseCase,
    private val confirmApplicationSubmission: ConfirmApplicationSubmissionUseCase,
    private val resetApplySession: ResetApplySessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplyAssistantUiState())

    /** Reactive UI state stream for [AiApplyScreen]. */
    public val uiState: StateFlow<ApplyAssistantUiState> = _uiState.asStateFlow()

    private var observedJobId: String? = null
    private var artifactJobCollector: Job? = null

    init {
        viewModelScope.launch {
            observeApplySession().collect { session ->
                _uiState.update { it.copy(session = session) }
                if (session != null && session.currentStep == ApplyWorkflowStep.DRAFTING) {
                    val jobId = session.artifactJob?.jobId
                    if (!jobId.isNullOrBlank() && jobId != observedJobId) {
                        reconnectArtifactObserver(session.id, jobId)
                    }
                }
            }
        }
    }

    /**
     * Processes a user intent [intent] from the Apply Assistant UI.
     */
    public fun onIntent(intent: ApplyAssistantUiIntent): Unit {
        when (intent) {
            is ApplyAssistantUiIntent.SelectInputMode ->
                _uiState.update { it.copy(inputMode = intent.mode, errorMessage = null) }

            is ApplyAssistantUiIntent.ChangeUrl ->
                _uiState.update { it.copy(urlInput = intent.value, errorMessage = null) }

            is ApplyAssistantUiIntent.ChangeDescription ->
                _uiState.update { it.copy(descriptionInput = intent.value, errorMessage = null) }

            is ApplyAssistantUiIntent.ChangeCompany ->
                _uiState.update { it.copy(companyInput = intent.value) }

            is ApplyAssistantUiIntent.ChangeRole ->
                _uiState.update { it.copy(roleInput = intent.value) }

            is ApplyAssistantUiIntent.SubmitEvaluation -> submitEvaluation()
            is ApplyAssistantUiIntent.RespondGenerateAssets -> respondGenerateAssets(intent.generate)
            is ApplyAssistantUiIntent.RespondSubmitted -> respondSubmitted(intent.submitted)
            is ApplyAssistantUiIntent.ResetSession -> handleReset()
            is ApplyAssistantUiIntent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun submitEvaluation() {
        val current = _uiState.value
        val targetUrl = current.urlInput.trim().takeIf { it.isNotBlank() && current.inputMode == ApplyInputMode.URL }
        val targetText = current.descriptionInput.trim().takeIf { it.isNotBlank() && current.inputMode == ApplyInputMode.DESCRIPTION }
        if (targetUrl == null && targetText == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid URL or job description.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isEvaluating = true, isAiRefining = false, errorMessage = null) }
            runCatching {
                evaluateJobPosting(
                    EvaluateJobPostingUseCase.Params(
                        url = targetUrl,
                        text = targetText,
                        company = current.companyInput.trim().takeIf { it.isNotBlank() },
                        role = current.roleInput.trim().takeIf { it.isNotBlank() },
                    )
                )
            }.onSuccess { session ->
                _uiState.update { it.copy(session = session, isEvaluating = false, isAiRefining = true) }
                launchAiCalibration(session.id)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isEvaluating = false,
                        isAiRefining = false,
                        errorMessage = err.message ?: "Failed to evaluate job posting",
                    )
                }
            }
        }
    }

    private fun launchAiCalibration(sessionId: String) {
        viewModelScope.launch {
            runCatching {
                refineJobEvaluationWithAi(sessionId)
            }.onSuccess { updated ->
                _uiState.update { it.copy(session = updated, isAiRefining = false) }
            }.onFailure {
                _uiState.update { it.copy(isAiRefining = false) }
            }
        }
    }

    private fun respondGenerateAssets(generate: Boolean) {
        val sessionId = _uiState.value.session?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPrompt = true, errorMessage = null) }
            runCatching {
                generateApplicationArtifacts(
                    GenerateApplicationArtifactsUseCase.Params(
                        sessionId = sessionId,
                        generate = generate,
                    )
                )
            }.onSuccess { updated ->
                _uiState.update { it.copy(session = updated, isSubmittingPrompt = false) }
                val jobId = updated.artifactJob?.jobId
                if (generate && !jobId.isNullOrBlank()) {
                    reconnectArtifactObserver(updated.id, jobId)
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSubmittingPrompt = false,
                        errorMessage = err.message ?: "Failed to process asset generation request",
                    )
                }
            }
        }
    }

    private fun reconnectArtifactObserver(sessionId: String, jobId: String) {
        observedJobId = jobId
        artifactJobCollector?.cancel()
        artifactJobCollector = viewModelScope.launch {
            observeArtifactJob(jobId)
                .catch { err ->
                    _uiState.update {
                        it.copy(errorMessage = err.message ?: "Artifact status stream interrupted")
                    }
                }
                .collect { jobState ->
                    if (jobState != null) {
                        val synced = runCatching {
                            syncDraftedArtifacts(
                                SyncDraftedArtifactsUseCase.Params(
                                    sessionId = sessionId,
                                    jobState = jobState,
                                )
                            )
                        }.getOrNull()
                        if (synced != null) {
                            _uiState.update { it.copy(session = synced) }
                        }
                        if (jobState.status == "SUCCESS" || jobState.status == "FAILED") {
                            artifactJobCollector?.cancel()
                        }
                    }
                }
        }
    }

    private fun respondSubmitted(submitted: Boolean) {
        val sessionId = _uiState.value.session?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPrompt = true, errorMessage = null) }
            runCatching {
                confirmApplicationSubmission(
                    ConfirmApplicationSubmissionUseCase.Params(
                        sessionId = sessionId,
                        submitted = submitted,
                    )
                )
            }.onSuccess { updated ->
                _uiState.update { it.copy(session = updated, isSubmittingPrompt = false) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSubmittingPrompt = false,
                        errorMessage = err.message ?: "Failed to update submission status",
                    )
                }
            }
        }
    }

    private fun handleReset() {
        observedJobId = null
        artifactJobCollector?.cancel()
        viewModelScope.launch {
            resetApplySession()
            _uiState.value = ApplyAssistantUiState()
        }
    }
}
