package dev.fnc314.tools.jobtracker.features.companydetails.data.repository.impl

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.CompanyMetrics
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.LogoFormat
import dev.fnc314.tools.jobtracker.core.data.model.LogoShape
import dev.fnc314.tools.jobtracker.core.data.model.Timeline
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.core.data.source.RemoteRecord
import dev.fnc314.tools.jobtracker.core.data.util.InstantParser
import dev.fnc314.tools.jobtracker.features.companydetails.data.repository.CompanyDetailsRepository
import dev.fnc314.tools.jobtracker.core.data.model.CompanyWithApplications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant

internal class CompanyDetailsRepositoryImpl(
    private val remoteDataSource: RemoteDataSource,
) : CompanyDetailsRepository {

    override suspend fun getCompanyDetails(companyId: String): CompanyWithApplications? {
        return withContext(Dispatchers.Default) {
            val companyRecord = remoteDataSource.getDocument("companies", companyId) ?: return@withContext null
            val company = mapRecordToCompany(companyRecord)

            val allApplications = remoteDataSource.getCollection("applications").map(::mapRecordToJobApplication)
            val matchedApplications = allApplications.filter { app ->
                val companyName = company.name.lowercase().trim()
                val companyIdLower = company.id.lowercase().trim()
                val appCompany = app.company.lowercase().trim()
                appCompany == companyName ||
                        appCompany == companyIdLower ||
                        company.aliases.any { it.lowercase().trim() == appCompany } ||
                        app.id.startsWith("$companyIdLower-")
            }

            CompanyWithApplications(
                company = company,
                applications = matchedApplications,
            )
        }
    }

    private fun mapRecordToCompany(record: RemoteRecord): Company {
        val aliases = (record.fields["aliases"] as? List<*>)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val logoFormatStr = (record.getString("logoFormat") ?: record.getString("logo_format")).orEmpty().uppercase()
        val logoShapeStr = (record.getString("logoShape") ?: record.getString("logo_shape")).orEmpty().uppercase()

        val logoFormat = runCatching { LogoFormat.valueOf(logoFormatStr) }.getOrDefault(LogoFormat.UNKNOWN)
        val logoShape = runCatching { LogoShape.valueOf(logoShapeStr) }.getOrDefault(LogoShape.UNKNOWN)

        val metricsMap = record.fields["metrics"] as? Map<*, *>
        val metrics = CompanyMetrics.fromMap(metricsMap)

        return Factory.createCompany(
            id = record.id,
            name = record.nonBlankString("name")
                ?: record.nonBlankString("company")
                ?: record.id,
            aliases = aliases,
            website = record.getString("website"),
            industry = record.getString("industry"),
            createdAt = InstantParser.parse(
                record.fields["createdAt"] ?: record.fields["created_at"],
            ),
            updatedAt = InstantParser.parse(
                record.fields["updatedAt"] ?: record.fields["updated_at"],
            ),
            logoUrl = record.getString("logoUrl"),
            logoFormat = logoFormat,
            logoWidth = (record.getLong("logoWidth") ?: record.getLong("logo_width"))?.toInt() ?: 0,
            logoHeight = (record.getLong("logoHeight") ?: record.getLong("logo_height"))?.toInt() ?: 0,
            logoShape = logoShape,
            logoFetchedAt = InstantParser.parse(record.fields["logoFetchedAt"]),
            metrics = metrics,
        )
    }

    private fun mapRecordToJobApplication(record: RemoteRecord): JobApplication {
        val timeline = parseTimeline(record.fields["timeline"] as? Map<*, *>)
        val createdAt = InstantParser.parse(record.fields["createdAt"] ?: record.fields["created_at"])
        val updatedAt = InstantParser.parse(record.fields["updatedAt"] ?: record.fields["updated_at"])
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
        getString(key)?.trim()?.takeIf { it.isNotEmpty() }
}
