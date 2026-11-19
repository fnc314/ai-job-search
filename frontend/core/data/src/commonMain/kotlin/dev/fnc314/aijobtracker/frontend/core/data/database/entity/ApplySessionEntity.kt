package dev.fnc314.aijobtracker.frontend.core.data.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Local persistence entity capturing the active state of an in-app Apply Assistant workflow session.
 *
 * Persisting session state locally guarantees survival across configuration changes, app
 * backgrounding, and Android process death while asynchronous evaluation and artifact generation
 * jobs complete on the backend.
 *
 * @property id Unique session identifier.
 * @property applicationId Canonical application slug/document identifier in remote storage, if created.
 * @property postingUrl Target job posting URL supplied by the user, if any.
 * @property postingText Raw job description text supplied or fetched for evaluation.
 * @property company Target hiring organization name.
 * @property role Target role title.
 * @property deterministicScore Tier 1 instant deterministic fit score (0..100).
 * @property aiScore Tier 2 AI-calibrated semantic fit score (0..100), or null while pending.
 * @property verdict Qualitative fit verdict string.
 * @property aiRationale Calibration explanation returned by the AI tier, if resolved.
 * @property strengthsJson JSON-encoded list of matched candidate strengths.
 * @property gapsJson JSON-encoded list of identified skill or experience gaps.
 * @property dealBreakersJson JSON-encoded list of detected deal-breakers.
 * @property artifactJobId Asynchronous backend job identifier for `artifact_jobs/{jobId}`.
 * @property artifactJobStatus Current asynchronous artifact job status (`IN_PROGRESS`, `SUCCESS`, `FAILED`).
 * @property cvPdfUrl Path or download URL for the drafted/compiled CV artifact.
 * @property coverLetterPdfUrl Path or download URL for the drafted/compiled cover letter artifact.
 * @property currentStep Current workflow step (`AWAITING_INPUT`, `EVALUATING`, `AWAITING_GENERATE_CONFIRMATION`, `DRAFTING`, `DRAFTED`, `AWAITING_SUBMISSION_CONFIRMATION`, `SUBMITTED`).
 * @property updatedAt Epoch milliseconds timestamp of the last session mutation.
 */
@Entity(tableName = "apply_sessions")
public data class ApplySessionEntity(
    @PrimaryKey public val id: String,
    public val applicationId: String? = null,
    public val postingUrl: String? = null,
    public val postingText: String? = null,
    public val company: String = "",
    public val role: String = "",
    public val deterministicScore: Int? = null,
    public val aiScore: Int? = null,
    public val verdict: String? = null,
    public val aiRationale: String? = null,
    public val strengthsJson: String? = null,
    public val gapsJson: String? = null,
    public val dealBreakersJson: String? = null,
    public val artifactJobId: String? = null,
    public val artifactJobStatus: String? = null,
    public val cvPdfUrl: String? = null,
    public val coverLetterPdfUrl: String? = null,
    public val currentStep: String = "AWAITING_INPUT",
    public val updatedAt: Long = 0L,
)
