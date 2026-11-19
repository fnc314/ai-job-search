package dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase

import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyArtifactJobState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplySessionState
import kotlinx.coroutines.flow.Flow

/**
 * Observes the currently active [ApplySessionState] persisted locally.
 */
public fun interface ObserveApplySessionUseCase {
    /**
     * Emits reactive snapshots of the active Apply Assistant session.
     *
     * @return Cold [Flow] emitting the active [ApplySessionState] or null.
     */
    public operator fun invoke(): Flow<ApplySessionState?>
}

/**
 * Evaluates a job posting deterministically via Tier 1 RankScorer and tracks it as `RANKED`.
 */
public fun interface EvaluateJobPostingUseCase {
    /**
     * Input parameters describing the target posting to evaluate.
     *
     * @property url Optional job posting URL.
     * @property text Optional raw job description text.
     * @property company Optional company name override.
     * @property role Optional role title override.
     */
    public data class Params(
        public val url: String? = null,
        public val text: String? = null,
        public val company: String? = null,
        public val role: String? = null,
    )

    /**
     * Runs deterministic evaluation and updates the persisted session.
     *
     * @param params Target job posting inputs.
     * @return Updated [ApplySessionState] containing deterministic evaluation results.
     */
    public suspend operator fun invoke(params: Params): ApplySessionState
}

/**
 * Runs asynchronous Tier 2 AI semantic ranking to refine the deterministic score of an active session.
 */
public fun interface RefineJobEvaluationWithAiUseCase {
    /**
     * Invokes `/ai/rank` calibration for [sessionId].
     *
     * @param sessionId Target session identifier.
     * @return Updated [ApplySessionState] with AI score and rationale.
     */
    public suspend operator fun invoke(sessionId: String): ApplySessionState
}

/**
 * Handles user confirmation on whether to generate tailored CV and cover letter assets.
 */
public fun interface GenerateApplicationArtifactsUseCase {
    /**
     * Input decision parameters for asset generation.
     *
     * @property sessionId Target session identifier.
     * @property generate True to initiate `DRAFTING`, false to leave record as `RANKED`.
     */
    public data class Params(
        public val sessionId: String,
        public val generate: Boolean,
    )

    /**
     * Triggers async artifact generation or declines cleanly.
     *
     * @param params Session identifier and decision flag.
     * @return Updated [ApplySessionState].
     */
    public suspend operator fun invoke(params: Params): ApplySessionState
}

/**
 * Observes remote `artifact_jobs/{jobId}` status for resilient recovery across process death.
 */
public fun interface ObserveArtifactJobUseCase {
    /**
     * Observes real-time progress for [jobId].
     *
     * @param jobId Asynchronous artifact job identifier.
     * @return Reactive [Flow] emitting [ApplyArtifactJobState] updates.
     */
    public operator fun invoke(jobId: String): Flow<ApplyArtifactJobState?>
}

/**
 * Applies a resolved [ApplyArtifactJobState] update to the local session and transitions status to `DRAFTED`.
 */
public fun interface SyncDraftedArtifactsUseCase {
    /**
     * Input payload containing resolved artifact job metadata.
     *
     * @property sessionId Target session identifier.
     * @property jobState Completed [ApplyArtifactJobState].
     */
    public data class Params(
        public val sessionId: String,
        public val jobState: ApplyArtifactJobState,
    )

    /**
     * Saves artifact links locally and updates remote application status to `DRAFTED`.
     *
     * @param params Session and completed artifact job snapshot.
     * @return Updated [ApplySessionState].
     */
    public suspend operator fun invoke(params: Params): ApplySessionState
}

/**
 * Records final submission confirmation (`Yes` -> `SUBMITTED`, `No` -> remains `DRAFTED`).
 */
public fun interface ConfirmApplicationSubmissionUseCase {
    /**
     * Input payload capturing submission confirmation.
     *
     * @property sessionId Target session identifier.
     * @property submitted True if candidate submitted the application.
     */
    public data class Params(
        public val sessionId: String,
        public val submitted: Boolean,
    )

    /**
     * Finalizes the workflow and updates remote status when submitted.
     *
     * @param params Session and submission decision.
     * @return Updated [ApplySessionState].
     */
    public suspend operator fun invoke(params: Params): ApplySessionState
}

/**
 * Resets the current Apply Assistant session so the user can start evaluating a new role.
 */
public fun interface ResetApplySessionUseCase {
    /**
     * Clears persisted session and chat history for a fresh evaluation.
     */
    public suspend operator fun invoke()
}
