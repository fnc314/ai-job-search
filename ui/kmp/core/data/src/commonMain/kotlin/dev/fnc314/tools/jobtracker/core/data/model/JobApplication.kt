package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant

/**
 * Central domain entity representing an active or archived job application within the job search pipeline.
 *
 * Grounded in Firestore document schema version 7 (`tracker.schema.json`), this record tracks the complete lifecycle
 * of an opportunity—from lead discovery and drafting, through interviews and negotiations, to final resolution.
 *
 * @property id The slug identifier (e.g., `"acme-corp-principal-android"`), historically matching Firestore document IDs.
 * @property uuid The time-ordered RFC 4122 Variant UUIDv7 identifier [Uuid7] acting as the primary key.
 * @property schemaVersion Document schema migration version number (current standard is `7`).
 * @property company The hiring company or employer name.
 * @property role The title of the target position (e.g., `"Principal Android Software Engineer"`).
 * @property location Human-readable summary string of the work location (e.g. `"Pittsburgh, PA / Remote"`).
 * @property status The current lifecycle state [TrackerStatus] of the application.
 * @property channel The sourcing platform or application vector [ApplicationChannel] (e.g., Greenhouse, Workday, Referral).
 * @property jobId External job board requisition identifier or posting number, if available.
 * @property jobUrl Web URL referencing the live job description or portal posting.
 * @property description Detailed text summary or raw job description.
 * @property tags Category tags and metadata flags classifying compensation bands, tech gaps, or process attributes.
 * @property contactIds Identifiers of recruiters, hiring managers, or team members linked to this process.
 * @property compensation Structured salary and total compensation expectations [Compensation].
 * @property locationDetail Structured geographic constraints, remote work eligibility, and relocation terms [Location].
 * @property fitRating Legacy integer fit score scalar (0 to 100), if present.
 * @property fitEvaluation Comprehensive heuristic fit analysis [TrackerFit] detailing alignment score and identified gaps.
 * @property counters Aggregated metrics [TrackerCounts] tracking touchpoints, follow-up messages, interviews, and rejections.
 * @property timeline Key milestone timestamps [Timeline] including initial outreach, last interaction, and scheduled next follow-up.
 * @property notesRendered Markdown-rendered human and agent notes summarizing strategic positioning or recruiter feedback.
 * @property createdAt Timestamp when the record was initially created.
 * @property updatedAt Timestamp when the record was last modified.
 * @property jobApplicationUrl Direct link to the submitted application or candidate portal dashboard.
 * @see TrackerStatus
 * @see ApplicationChannel
 * @see TrackerFit
 * @see Timeline
 * @see Compensation
 * @see Location
 * @see Uuid7
 */
@Stable
public data class JobApplication(
    public val id: String,
    public val uuid: Uuid7? = null,
    public val schemaVersion: Int = 7,
    public val company: String,
    public val role: String?,
    public val location: String?,
    public val status: TrackerStatus,
    public val channel: ApplicationChannel,
    public val jobId: String?,
    public val jobUrl: String?,
    public val description: String?,
    public val tags: ImmutableList<String>,
    public val contactIds: ImmutableList<String>,
    public val compensation: Compensation?,
    public val locationDetail: Location?,
    public val fitRating: Int? = null,
    public val fitEvaluation: TrackerFit?,
    public val counters: TrackerCounts?,
    public val timeline: Timeline?,
    public val notesRendered: String?,
    public val createdAt: Instant?,
    public val updatedAt: Instant?,
    public val jobApplicationUrl: String?,
) {
    /**
     * Resolves the primary fit score (0..100) by prioritizing [fitRating] over [fitEvaluation.score].
     */
    public val fitScore: Int?
        get() = fitRating ?: fitEvaluation?.score
}
