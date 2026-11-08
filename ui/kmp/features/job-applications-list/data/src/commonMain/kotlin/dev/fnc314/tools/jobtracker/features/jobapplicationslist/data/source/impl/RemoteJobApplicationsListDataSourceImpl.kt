package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.RemoteJobApplicationsListDataSource
import kotlin.time.Instant
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
          val createdAt = record.fields["createdAt"]?.toInstantOrNull()
            ?: record.fields["created_at"]?.toInstantOrNull()
          val updatedAt = record.fields["updatedAt"]?.toInstantOrNull()
            ?: record.fields["updated_at"]?.toInstantOrNull()

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
            notesRendered = record.getString("notesRendered"),
            createdAt = createdAt,
            updatedAt = updatedAt,
          )
        }
    }
  }
}

private fun Any?.toInstantOrNull(): Instant? {
  return when (this) {
    null -> null
    is Instant -> this
    is Long -> Instant.fromEpochMilliseconds(this)
    is String -> runCatching { Instant.parse(this) }.getOrNull()
    else -> runCatching { Instant.parse(this.toString()) }.getOrNull()
  }
}