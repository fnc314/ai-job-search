package dev.fnc314.aijobtracker.frontend.features.aiapply.domain.repository

import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyArtifactJobState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplySessionState
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository coordinating local session persistence, deterministic and AI job evaluation,
 * asynchronous artifact generation, and real-time remote status updates.
 */
public interface ApplyAssistantRepository {
    /**
     * Observes the currently active [ApplySessionState] from local storage.
     *
     * @return Reactive [Flow] emitting session updates.
     */
    public fun observeLatestSession(): Flow<ApplySessionState?>

    /**
     * Evaluates a job posting deterministically via Tier 1 RankScorer, creates/updates the
     * record in remote storage with status `RANKED`, and persists the session locally.
     *
     * @param url Optional job posting URL.
     * @param text Optional raw job description text.
     * @param company Optional company override.
     * @param role Optional role title override.
     * @return Updated [ApplySessionState] after Tier 1 evaluation.
     */
    public suspend fun evaluateDeterministic(
        url: String?,
        text: String?,
        company: String?,
        role: String?,
    ): ApplySessionState

    /**
     * Invokes Tier 2 AI semantic ranking to refine the deterministic score and rationale for [sessionId].
     *
     * @param sessionId Target session identifier.
     * @return Updated [ApplySessionState] containing AI score and rationale.
     */
    public suspend fun refineWithAi(sessionId: String): ApplySessionState

    /**
     * Responds to the "Generate assets?" confirmation prompt.
     *
     * Selecting true transitions the application to `DRAFTING` and triggers background artifact generation;
     * selecting false retains `RANKED` status and concludes the workflow.
     *
     * @param sessionId Target session identifier.
     * @param generate True to start background LaTeX/PDF drafting, false to keep `RANKED`.
     * @return Updated [ApplySessionState].
     */
    public suspend fun respondToGenerateAssets(
        sessionId: String,
        generate: Boolean,
    ): ApplySessionState

    /**
     * Observes remote `artifact_jobs/{jobId}` progress for resilience across backgrounding and process death.
     *
     * @param jobId Asynchronous artifact job identifier.
     * @return Reactive [Flow] emitting [ApplyArtifactJobState] snapshots.
     */
    public fun observeArtifactJob(jobId: String): Flow<ApplyArtifactJobState?>

    /**
     * Applies completed [jobState] artifacts to the local session and ensures application status is `DRAFTED`.
     *
     * @param sessionId Target session identifier.
     * @param jobState Resolved [ApplyArtifactJobState] from remote listener or polling.
     * @return Updated [ApplySessionState].
     */
    public suspend fun applyArtifactJobUpdate(
        sessionId: String,
        jobState: ApplyArtifactJobState,
    ): ApplySessionState

    /**
     * Responds to the "Did you submit the application?" confirmation prompt.
     *
     * @param sessionId Target session identifier.
     * @param submitted True to transition to `SUBMITTED`, false to retain `DRAFTED`.
     * @return Updated [ApplySessionState].
     */
    public suspend fun confirmSubmission(
        sessionId: String,
        submitted: Boolean,
    ): ApplySessionState

    /**
     * Clears the active local Apply Assistant session to start a new evaluation.
     */
    public suspend fun resetSession()
}
