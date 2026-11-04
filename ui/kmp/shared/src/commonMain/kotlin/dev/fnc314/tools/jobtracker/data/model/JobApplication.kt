package dev.fnc314.tools.jobtracker.data.model

import kotlin.time.Instant

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
    val tags: List<String>,
    val contactIds: List<String>,
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
