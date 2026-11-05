package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant

@Stable
public data class JobApplication(
    public val id: String,
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
    public val fitScore: Int?
        get() = fitRating ?: fitEvaluation?.score
}
