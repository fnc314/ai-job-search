package dev.fnc314.tools.jobtracker.core.data.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlin.time.Instant

/**
 * Room 3 entity representing a tracked job application in the local SQLite relational database cache.
 *
 * Conforms to `tracker.schema.json` (schemaVersion = 8).
 * Uses canonical time-ordered [uuid] as the primary key with [slugId] uniquely indexed for fast lookups.
 *
 * @property uuid Primary key [Uuid7] ensuring monotonic sorting and unique entity identity.
 * @property slugId Canonical slug identifier (e.g. `"stripe-staff-android"`), indexed uniquely.
 * @property schemaVersion Document schema version number (defaults to `7`).
 * @property company Hiring organization name.
 * @property role Position title.
 * @property location Work location summary string.
 * @property status Current lifecycle status [TrackerStatus].
 * @property channel Originating ATS channel [ApplicationChannel].
 * @property jobId External posting requisition identifier.
 * @property jobUrl Live web posting URL.
 * @property description Detailed role overview or job description.
 * @property tags Categorization tags serialized as a comma-separated column.
 * @property contactIds Linked recruiter or referrer identifiers serialized as a comma-separated column.
 * @property fitScore Calculated alignment rating (0..100).
 * @property notesRendered Markdown notes content.
 * @property createdAt Timestamp when the record was initially created.
 * @property updatedAt Timestamp when the record was last modified.
 * @property jobApplicationUrl Direct candidate portal link.
 * @property submittedAt Timestamp when the application was submitted
 * @property submissionConfirmedAt Timestamp when the application submission was confirmed
 */
@Entity(
    tableName = "job_applications",
    indices = [
        Index(value = ["slugId"], unique = true)
    ]
)
public data class JobApplicationEntity(
    @PrimaryKey public val uuid: Uuid7,
    public val slugId: String,
    public val schemaVersion: Int = 8,
    public val company: String,
    public val role: String?,
    public val location: String?,
    public val status: TrackerStatus,
    public val channel: ApplicationChannel,
    public val jobId: String?,
    public val jobUrl: String?,
    public val description: String?,
    public val tags: List<String>,
    public val contactIds: List<String>,
    public val fitScore: Int?,
    public val notesRendered: String?,
    public val createdAt: Instant?,
    public val updatedAt: Instant?,
    public val jobApplicationUrl: String?,
    public val emailMessageIds: List<String> = emptyList(),
    public val submittedAt: Instant?,
    public val submissionConfirmedAt: Instant? = null,
)
