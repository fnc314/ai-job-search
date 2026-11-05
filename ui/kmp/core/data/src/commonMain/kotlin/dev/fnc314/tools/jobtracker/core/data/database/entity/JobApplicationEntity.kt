package dev.fnc314.tools.jobtracker.core.data.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlin.time.Instant

/**
 * Room 3 entity representing a tracked job application in the local SQLite database.
 *
 * Conforms to `tracker.schema.json` (schemaVersion = 7).
 * Uses canonical [uuid] as the primary key with [slugId] indexed for human-readable query lookup.
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
    public val schemaVersion: Int = 7,
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
)
