package dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus

/**
 * Input mode pre-selected or toggled in the Apply Assistant input form.
 */
public enum class ApplyInputMode {
    /** Accepts an HTTPS job posting URL for scraping and evaluation. */
    URL,

    /** Accepts raw Markdown or plain-text job description content. */
    DESCRIPTION,
}

/**
 * Ordered steps progressing through the interactive Apply Assistant workflow.
 */
public enum class ApplyWorkflowStep {
    /** Waiting for the user to submit a job posting URL or description. */
    AWAITING_INPUT,

    /** Deterministic fit evaluation is running. */
    EVALUATING,

    /** Fit score evaluated (RANKED); prompting whether to generate tailored assets. */
    AWAITING_GENERATE_CONFIRMATION,

    /** Asynchronous LaTeX drafting and PDF compilation underway (DRAFTING). */
    DRAFTING,

    /** Tailored artifacts completed and attached (DRAFTED); prompting for submission. */
    DRAFTED,

    /** Prompting whether the user formally submitted the application. */
    AWAITING_SUBMISSION_CONFIRMATION,

    /** Application confirmed as submitted (SUBMITTED) or session concluded cleanly. */
    SUBMITTED,
}

/**
 * Originator role of a message item in the Apply Assistant conversation timeline.
 */
public enum class ApplyMessageSender {
    /** Message or prompt submitted by the candidate. */
    USER,

    /** Automated response or interactive card from the assistant. */
    ASSISTANT,

    /** Lifecycle transition or system status note. */
    SYSTEM,
}

/**
 * Structured card variants rendered inside the Apply Assistant chat timeline.
 */
public enum class ApplyMessageType {
    /** Plain conversational bubble. */
    TEXT,

    /** Fit evaluation breakdown card featuring animated dual-tier score transition. */
    EVALUATION_CARD,

    /** Interactive Yes/No confirmation prompt. */
    YES_NO_PROMPT,

    /** Generated tailored CV and cover letter PDF links card. */
    ARTIFACTS_CARD,
}

/**
 * Dual-tier evaluation breakdown combining Tier 1 deterministic RankScorer output
 * with asynchronous Tier 2 AI semantic calibration.
 *
 * @property applicationId Canonical Firestore application identifier tracked as `RANKED`.
 * @property company Target employer name.
 * @property role Target job role title.
 * @property score Active displayed fit score (transitions from [deterministicScore] to [aiScore]).
 * @property deterministicScore Tier 1 instant deterministic fit score (0..100).
 * @property aiScore Tier 2 AI-calibrated score once resolved, or null while pending.
 * @property isAiEvaluated True after Tier 2 AI calibration has completed.
 * @property verdict Qualitative fit rating label.
 * @property aiRationale Calibration explanation provided by AI tier.
 * @property strengths Identified candidate strengths matched to the posting.
 * @property gaps Identified skill or experience gaps.
 * @property dealBreakers Identified role deal-breakers.
 * @property status Current application [TrackerStatus].
 */
public data class ApplyEvaluationResult(
    public val applicationId: String,
    public val company: String,
    public val role: String,
    public val score: Int,
    public val deterministicScore: Int,
    public val aiScore: Int? = null,
    public val isAiEvaluated: Boolean = false,
    public val verdict: String,
    public val aiRationale: String = "",
    public val strengths: List<String> = emptyList(),
    public val gaps: List<String> = emptyList(),
    public val dealBreakers: List<String> = emptyList(),
    public val status: TrackerStatus = TrackerStatus.RANKED,
)

/**
 * Live state of a background artifact generation job observed across local persistence and remote listeners.
 *
 * @property jobId Unique backend job identifier under `artifact_jobs/{jobId}`.
 * @property applicationId Associated Firestore application identifier.
 * @property status Job status (`IN_PROGRESS`, `SUCCESS`, `FAILED`).
 * @property cvPdfUrl Path or URL to tailored CV artifact.
 * @property coverLetterPdfUrl Path or URL to tailored cover letter artifact.
 * @property error Failure reason if [status] is `FAILED`.
 * @property cvArtifactId Firestore artifact identifier for the tailored CV under `applications/{id}/artifacts`.
 * @property coverLetterArtifactId Firestore artifact identifier for the cover letter under `applications/{id}/artifacts`.
 * @property outcomeArtifactId Firestore artifact identifier for the evaluation `outcome.md` record.
 * @property outcomeUrl Path or URL to the fit evaluation `outcome.md` artifact.
 */
public data class ApplyArtifactJobState(
    public val jobId: String,
    public val applicationId: String,
    public val status: String,
    public val cvPdfUrl: String? = null,
    public val coverLetterPdfUrl: String? = null,
    public val error: String? = null,
    public val cvArtifactId: String? = null,
    public val coverLetterArtifactId: String? = null,
    public val outcomeArtifactId: String? = null,
    public val outcomeUrl: String? = null,
)

/**
 * Timeline entry displayed inside the Apply Assistant chat interface.
 *
 * @property id Unique message identifier.
 * @property sender Originating actor [ApplyMessageSender].
 * @property messageType Structural presentation variant [ApplyMessageType].
 * @property text Primary rendered message text.
 * @property evaluation Evaluation snapshot if [messageType] is [ApplyMessageType.EVALUATION_CARD].
 * @property promptStep Workflow prompt step if [messageType] is [ApplyMessageType.YES_NO_PROMPT].
 * @property artifactJob Artifact job snapshot if [messageType] is [ApplyMessageType.ARTIFACTS_CARD].
 * @property timestamp Epoch milliseconds creation timestamp.
 */
public data class ApplyChatMessage(
    public val id: String,
    public val sender: ApplyMessageSender,
    public val messageType: ApplyMessageType,
    public val text: String,
    public val evaluation: ApplyEvaluationResult? = null,
    public val promptStep: ApplyWorkflowStep? = null,
    public val artifactJob: ApplyArtifactJobState? = null,
    public val timestamp: Long = 0L,
)

/**
 * Complete snapshot of an active Apply Assistant workflow session stored locally so it survives
 * configuration changes, backgrounding, and process death.
 *
 * @property id Unique session identifier.
 * @property inputMode Selected input mode ([ApplyInputMode.URL] or [ApplyInputMode.DESCRIPTION]).
 * @property postingUrl Entered posting URL, if any.
 * @property postingText Entered raw job description text, if any.
 * @property company Optional company override or resolved company name.
 * @property role Optional role override or resolved role title.
 * @property evaluation Latest [ApplyEvaluationResult], if evaluated.
 * @property artifactJob Active or completed [ApplyArtifactJobState], if drafting started.
 * @property currentStep Current [ApplyWorkflowStep].
 * @property messages Chronological list of [ApplyChatMessage] items.
 * @property updatedAt Epoch milliseconds last update timestamp.
 */
public data class ApplySessionState(
    public val id: String,
    public val inputMode: ApplyInputMode = ApplyInputMode.DESCRIPTION,
    public val postingUrl: String = "",
    public val postingText: String = "",
    public val company: String = "",
    public val role: String = "",
    public val evaluation: ApplyEvaluationResult? = null,
    public val artifactJob: ApplyArtifactJobState? = null,
    public val currentStep: ApplyWorkflowStep = ApplyWorkflowStep.AWAITING_INPUT,
    public val messages: List<ApplyChatMessage> = emptyList(),
    public val updatedAt: Long = 0L,
)
