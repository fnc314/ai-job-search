package dev.fnc314.tools.jobtracker.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant

@Stable
data class JobApplication(
    val id: String,
    val company: String,
    val role: String?,
    val location: String?,
    val status: TrackerStatus,
    val channel: ApplicationChannel,
    val jobId: String?,
    val jobUrl: String?,
    val description: String?,
    val tags: ImmutableList<String>,
    val contactIds: ImmutableList<String>,
    val compensation: Compensation?,
    val locationDetail: Location?,
    val fitEvaluation: TrackerFit?,
    val counters: TrackerCounts?,
    val timeline: Timeline?,
    val notesRendered: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val jobApplicationUrl: String?,
)
