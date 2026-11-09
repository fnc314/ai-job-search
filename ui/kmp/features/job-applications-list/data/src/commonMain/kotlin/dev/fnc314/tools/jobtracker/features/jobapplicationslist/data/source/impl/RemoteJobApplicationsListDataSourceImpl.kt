package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.Timeline
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.core.data.util.InstantParser
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.RemoteJobApplicationsListDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class RemoteJobApplicationsListDataSourceImpl(
  private val remoteDataSource: RemoteDataSource,
) : RemoteJobApplicationsListDataSource {
  override suspend operator fun invoke(): List<JobApplication> {
    return withContext(Dispatchers.Default) {
      remoteDataSource
        .observeCollection("applications")
        .first()
        .map { record ->
          val createdAt = InstantParser.parse(record.fields["createdAt"] ?: record.fields["created_at"])
          val updatedAt = InstantParser.parse(record.fields["updatedAt"] ?: record.fields["updated_at"])

          val timeline = (record.fields["timeline"] as? Map<*, *>)?.let { map ->
            Timeline(
              firstActivityAt = InstantParser.parse(map["firstActivityAt"] ?: map["first_activity_at"]),
              lastActivityAt = InstantParser.parse(map["lastActivityAt"] ?: map["last_activity_at"]),
              daysSinceActivity = ((map["daysSinceActivity"] ?: map["days_since_activity"]) as? Number)?.toInt() ?: 0,
              nextFollowUpAt = InstantParser.parse(map["nextFollowUpAt"] ?: map["next_follow_up_at"]),
            )
          }

          Factory.createJobApplication(
            id = record.id,
            company = record.getString("company") ?: record.getString("name") ?: "Unknown Company",
            role = record.getString("role") ?: record.getString("title"),
            location = record.getString("location"),
            status = TrackerStatus.fromFirestore(record.getString("status")),
            channel = ApplicationChannel.fromFirestore(record.getString("channel")),
            jobId = record.getString("jobId"),
            jobUrl = record.getString("jobUrl"),
            description = record.getString("description"),
            fitRating = record.getLong("fitScore")?.toInt() ?: record.getLong("fitRating")?.toInt(),
            timeline = timeline,
            notesRendered = record.getString("notesRendered"),
            createdAt = createdAt,
            updatedAt = updatedAt,
          )
        }
    }
  }
}
