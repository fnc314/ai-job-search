package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.Timeline
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.core.data.source.RemoteRecord
import dev.fnc314.tools.jobtracker.core.data.util.InstantParser
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.RemoteJobApplicationsListDataSource
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class RemoteJobApplicationsListDataSourceImpl(
  private val remoteDataSource: RemoteDataSource,
) : RemoteJobApplicationsListDataSource {
  override suspend operator fun invoke(): List<JobApplication> {
    return withContext(Dispatchers.Default) {
      remoteDataSource
        .getCollection("applications")
        .map(::mapRecordToJobApplication)
    }
  }

  private fun mapRecordToJobApplication(record: RemoteRecord): JobApplication {
    val timeline = parseTimeline(record.fields["timeline"] as? Map<*, *>)
    val createdAt = resolveCreatedAt(record)
    val updatedAt = resolveUpdatedAt(record, timeline, createdAt)
    val submittedAt = resolveSubmittedAt(record)
    val submissionConfirmedAt = resolveSubmissionConfirmedAt(record)

    return Factory.createJobApplication(
      id = record.id,
      company = record.nonBlankString("company") ?: record.nonBlankString("name") ?: "Unknown Company",
      role = record.nonBlankString("role") ?: record.nonBlankString("title"),
      location = record.nonBlankString("location"),
      status = TrackerStatus.fromFirestore(record.getString("status")),
      channel = ApplicationChannel.fromFirestore(record.getString("channel")),
      jobId = record.getString("jobId"),
      jobUrl = record.getString("jobUrl"),
      description = record.getString("description"),
      fitRating = (record.getLong("fitScore") ?: record.getLong("fitRating"))?.toInt(),
      timeline = timeline,
      notesRendered = record.getString("notesRendered"),
      createdAt = createdAt,
      updatedAt = updatedAt,
      submittedAt = submittedAt,
      submissionConfirmedAt = submissionConfirmedAt,
    )
  }

  private fun parseTimeline(map: Map<*, *>?): Timeline? {
    if (map == null) return null
    return Timeline(
      firstActivityAt = InstantParser.parse(map["firstActivityAt"] ?: map["first_activity_at"]),
      lastActivityAt = InstantParser.parse(map["lastActivityAt"] ?: map["last_activity_at"]),
      daysSinceActivity = ((map["daysSinceActivity"] ?: map["days_since_activity"]) as? Number)?.toInt() ?: 0,
      nextFollowUpAt = InstantParser.parse(map["nextFollowUpAt"] ?: map["next_follow_up_at"]),
    )
  }

  private fun resolveCreatedAt(record: RemoteRecord): Instant {
    val rawCreatedAt = InstantParser.parse(record.fields["createdAt"] ?: record.fields["created_at"])
    return if (rawCreatedAt != Instant.DISTANT_PAST) {
      rawCreatedAt
    } else {
      InstantParser.parse(record.fields["date"])
    }
  }

  private fun resolveUpdatedAt(record: RemoteRecord, timeline: Timeline?, createdAt: Instant): Instant {
    val rawUpdatedAt = InstantParser.parse(record.fields["updatedAt"] ?: record.fields["updated_at"])
    return when {
      rawUpdatedAt != Instant.DISTANT_PAST -> rawUpdatedAt
      timeline != null && timeline.lastActivityAt != Instant.DISTANT_PAST -> timeline.lastActivityAt
      else -> createdAt
    }
  }

  private fun resolveSubmittedAt(record: RemoteRecord): Instant? {
    val submittedAt = record.getInstant("submittedAt")
    if (submittedAt != Instant.DISTANT_PAST) {
      return submittedAt
    }
    val legacyDate = record.getInstant("date")
    return if (legacyDate != Instant.DISTANT_PAST) legacyDate else null
  }

  private fun resolveSubmissionConfirmedAt(record: RemoteRecord): Instant? {
    val confirmedAt = record.getInstant("submissionConfirmedAt")
    return if (confirmedAt != Instant.DISTANT_PAST) confirmedAt else null
  }

  private fun RemoteRecord.nonBlankString(key: String): String? =
    getString(key)?.takeIf { it.isNotBlank() }
}
