package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.LogoFormat
import dev.fnc314.tools.jobtracker.core.data.model.LogoShape
import dev.fnc314.tools.jobtracker.core.data.model.Timeline
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.source.MapRemoteRecord
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.core.data.source.RemoteRecord
import dev.fnc314.tools.jobtracker.core.data.util.InstantParser
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.RemoteJobApplicationDetailsDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant

internal class RemoteJobApplicationDetailsDataSourceImpl(
  private val remoteDataSource: RemoteDataSource,
) : RemoteJobApplicationDetailsDataSource {

  override suspend fun getJobApplication(id: String): JobApplication? = withContext(Dispatchers.Default) {
    val record = remoteDataSource.getDocument("applications", id)
    if (record != null) {
      return@withContext mapRecordToJobApplication(record)
    }
    val all = remoteDataSource.getCollection("applications")
    val fallback = all.find { it.id == id || it.getString("slugId") == id || it.getString("uuid") == id }
    fallback?.let(::mapRecordToJobApplication)
  }

  override suspend fun getCompanies(): List<Company> = withContext(Dispatchers.Default) {
    remoteDataSource.getCollection("companies").map(::mapRecordToCompany)
  }

  override suspend fun updateStatus(
    id: String,
    status: TrackerStatus,
    date: String?,
    note: String?,
  ): JobApplication = withContext(Dispatchers.Default) {
    val fields = mutableMapOf<String, Any?>(
      "status" to status.toFirestore(),
    )
    if (date != null) fields["statusDate"] = date
    if (note != null) fields["statusNote"] = note

    remoteDataSource.updateDocument(
      collectionPath = "applications",
      documentId = id,
      record = MapRemoteRecord(id = id, fields = fields),
    )
    getJobApplication(id)
      ?: Factory.createJobApplication(
        id = id,
        company = "",
        status = status,
        notesRendered = note,
      )
  }

  override suspend fun recordEvent(event: TrackerEvent): String = withContext(Dispatchers.Default) {
    val fields = mutableMapOf<String, Any?>(
      "applicationId" to event.applicationId,
      "type" to event.type.toFirestore(),
      "actor" to event.actor.toFirestore(),
      "derivedFrom" to event.derivedFrom.toFirestore(),
      "confidence" to event.confidence.toFirestore(),
      "createdAt" to event.createdAt.toString(),
    )
    event.statusFrom?.let { fields["statusFrom"] = it.toFirestore() }
    event.statusTo?.let { fields["statusTo"] = it.toFirestore() }
    event.note?.let { fields["note"] = it }
    event.emailMessageId?.let { fields["emailMessageId"] = it }

    remoteDataSource.setDocument(
      collectionPath = "events",
      documentId = event.id,
      record = MapRemoteRecord(id = event.id, fields = fields),
    )
    event.id
  }

  override suspend fun updateFitScore(id: String, score: Int): JobApplication = withContext(Dispatchers.Default) {
    val fields = mapOf<String, Any?>(
      "fitScore" to score.toLong(),
      "fitRating" to score.toLong(),
    )
    remoteDataSource.updateDocument(
      collectionPath = "applications",
      documentId = id,
      record = MapRemoteRecord(id = id, fields = fields),
    )
    getJobApplication(id)
      ?: Factory.createJobApplication(
        id = id,
        company = "",
        fitRating = score,
        fitEvaluation = Factory.createTrackerFit(score = score),
      )
  }

  private fun mapRecordToJobApplication(record: RemoteRecord): JobApplication {
    val timeline = parseTimeline(record.fields["timeline"] as? Map<*, *>)
    val createdAt = resolveCreatedAt(record)
    val updatedAt = resolveUpdatedAt(record, timeline, createdAt)
    val submittedAt = resolveSubmittedAt(record = record)
    val submissionConfirmedAt = resolveSubmissionConfirmedAt(record = record)

    @Suppress("UNCHECKED_CAST")
    val tags = (record.fields["tags"] as? List<String>)
      ?: emptyList()

    return Factory.createJobApplication(
      id = record.id,
      company = record.nonBlankString("company")
        ?: record.nonBlankString("name")
        ?: "Unknown Company",
      role = record.nonBlankString("role")
        ?: record.nonBlankString("title"),
      location = record.nonBlankString("location"),
      status = TrackerStatus.fromFirestore(record.getString("status")),
      channel = ApplicationChannel.fromFirestore(record.getString("channel")),
      jobId = record.getString("jobId"),
      jobUrl = record.getString("jobUrl"),
      description = record.getString("description"),
      tags = tags,
      fitRating = (record.getLong("fitScore")
        ?: record.getLong("fitRating"))?.toInt(),
      timeline = timeline,
      notesRendered = record.getString("notesRendered"),
      createdAt = createdAt,
      updatedAt = updatedAt,
      jobApplicationUrl = record.getString("jobApplicationUrl"),
      submittedAt = submittedAt,
      submissionConfirmedAt = submissionConfirmedAt,
    )
  }

  private fun mapRecordToCompany(record: RemoteRecord): Company {
    @Suppress("UNCHECKED_CAST")
    val aliases = (record.fields["aliases"] as? List<String>)
      ?: emptyList()
    val logoFormatStr = record.getString("logoFormat")?.uppercase()
      ?: "UNKNOWN"
    val logoShapeStr = record.getString("logoShape")?.uppercase()
      ?: "UNKNOWN"

    val logoFormat = runCatching { LogoFormat.valueOf(logoFormatStr) }.getOrDefault(LogoFormat.UNKNOWN)
    val logoShape = runCatching { LogoShape.valueOf(logoShapeStr) }.getOrDefault(LogoShape.UNKNOWN)

    return Factory.createCompany(
      id = record.id,
      name = record.nonBlankString("name")
        ?: record.nonBlankString("company")
        ?: record.id,
      aliases = aliases,
      website = record.getString("website"),
      industry = record.getString("sector") ?: record.getString("industry"),
      createdAt = InstantParser.parse(
        record.fields["createdAt"]
          ?: record.fields["created_at"],
      ),
      updatedAt = InstantParser.parse(
        record.fields["updatedAt"]
          ?: record.fields["updated_at"],
      ),
      logoUrl = record.getString("logoUrl"),
      logoFormat = logoFormat,
      logoWidth = record.getLong("logoWidth")?.toInt()
        ?: 0,
      logoHeight = record.getLong("logoHeight")?.toInt()
        ?: 0,
      logoShape = logoShape,
      logoFetchedAt = InstantParser.parse(record.fields["logoFetchedAt"]),
      metrics = dev.fnc314.tools.jobtracker.core.data.model.CompanyMetrics.fromMap(record.fields["metrics"] as? Map<*, *>),
      linkedinUrl = record.getString("linkedinUrl"),
    )
  }

  private fun parseTimeline(map: Map<*, *>?): Timeline? {
    if (map == null) return null
    return Timeline(
      firstActivityAt = InstantParser.parse(
        map["firstActivityAt"]
          ?: map["first_activity_at"],
      ),
      lastActivityAt = InstantParser.parse(
        map["lastActivityAt"]
          ?: map["last_activity_at"],
      ),
      daysSinceActivity = ((map["daysSinceActivity"]
        ?: map["days_since_activity"]) as? Number)?.toInt()
        ?: 0,
      nextFollowUpAt = InstantParser.parse(
        map["nextFollowUpAt"]
          ?: map["next_follow_up_at"],
      ),
    )
  }

  private fun resolveCreatedAt(record: RemoteRecord): Instant {
    val rawCreatedAt = InstantParser.parse(
      record.fields["createdAt"]
        ?: record.fields["created_at"],
    )
    return if (rawCreatedAt != Instant.DISTANT_PAST) {
      rawCreatedAt
    } else {
      InstantParser.parse(record.fields["date"])
    }
  }

  private fun resolveUpdatedAt(record: RemoteRecord, timeline: Timeline?, createdAt: Instant): Instant {
    val rawUpdatedAt = InstantParser.parse(
      record.fields["updatedAt"]
        ?: record.fields["updated_at"],
    )
    return when {
      rawUpdatedAt != Instant.DISTANT_PAST -> rawUpdatedAt
      timeline != null && timeline.lastActivityAt != Instant.DISTANT_PAST -> timeline.lastActivityAt
      else -> createdAt
    }
  }

  private fun resolveSubmittedAt(record: RemoteRecord): Instant {
    val submittedAt = record.getInstant("submittedAt")
    if (submittedAt != Instant.DISTANT_PAST) {
      return submittedAt
    }
    return when (val rawData = record.getInstant("date")) {
      Instant.DISTANT_PAST -> Instant.DISTANT_FUTURE
      else -> rawData
    }
  }

  private fun resolveSubmissionConfirmedAt(record: RemoteRecord): Instant? {
    val confirmedAt = record.getInstant("submissionConfirmedAt")
    return if (confirmedAt != Instant.DISTANT_PAST) confirmedAt else null
  }

  private fun RemoteRecord.nonBlankString(key: String): String? =
    getString(key)?.takeIf { it.isNotBlank() }
}
