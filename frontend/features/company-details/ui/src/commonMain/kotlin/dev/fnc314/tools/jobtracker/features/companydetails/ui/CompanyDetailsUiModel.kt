package dev.fnc314.tools.jobtracker.features.companydetails.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.widgets.JobApplicationCardUiModel

@Immutable
public data class CompanyDetailsUiModel(
    public val companyId: String,
    public val companyName: String,
    public val website: String? = null,
    public val industry: String? = null,
    public val logoUrl: String? = null,
    public val linkedinUrl: String? = null,
    public val companyInitial: String = companyName.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?",
    public val averageFitScore: Double = 0.0,
    public val averageFitScoreText: String = if (averageFitScore > 0.0) "${averageFitScore.toInt()}" else "—",
    public val salaryRangeDisplay: String = "",
    public val primaryChannelDisplay: String = "",
    public val locationsText: String = "",
    public val draftCount: Int = 0,
    public val draftPercentage: Float = 0f,
    public val submittedCount: Int = 0,
    public val submittedPercentage: Float = 0f,
    public val interviewingCount: Int = 0,
    public val interviewingPercentage: Float = 0f,
    public val offerCount: Int = 0,
    public val offerPercentage: Float = 0f,
    public val rejectedCount: Int = 0,
    public val rejectedPercentage: Float = 0f,
    public val applications: List<JobApplicationCardUiModel> = emptyList(),
)

public fun toCompanyDetailsUiModel(
    company: Company,
    applications: List<JobApplication>,
): CompanyDetailsUiModel {
    val m = company.metrics
    val counts = m?.counts
    val percentages = m?.percentages

    val applicationCardUiModels = applications.map { app ->
        JobApplicationCardUiModel(
            id = app.id,
            companyName = app.company,
            roleTitle = app.role ?: "Role Unavailable",
            locationText = app.location,
            status = app.status,
            statusDisplayName = app.status.displayName,
            fitScore = app.fitRating,
            fitScoreText = app.fitRating?.let { "Fit $it" },
            createdAt = app.createdAt,
            updatedAt = app.updatedAt,
            firstActivityAt = app.timeline?.firstActivityAt ?: kotlin.time.Instant.DISTANT_PAST,
            logoUrl = company.logoUrl,
        )
    }

    return CompanyDetailsUiModel(
        companyId = company.id,
        companyName = company.name,
        website = company.website,
        industry = company.industry,
        logoUrl = company.logoUrl,
        linkedinUrl = company.linkedinUrl,
        averageFitScore = m?.averageFitScore ?: 0.0,
        salaryRangeDisplay = m?.salaryRange?.display.orEmpty(),
        primaryChannelDisplay = m?.primaryChannel?.displayName().orEmpty(),
        locationsText = m?.locations?.joinToString(", ").orEmpty(),
        draftCount = counts?.draft ?: 0,
        draftPercentage = percentages?.draft ?: 0f,
        submittedCount = counts?.submitted ?: 0,
        submittedPercentage = percentages?.submitted ?: 0f,
        interviewingCount = counts?.interview ?: 0,
        interviewingPercentage = percentages?.interview ?: 0f,
        offerCount = counts?.offer ?: 0,
        offerPercentage = percentages?.offer ?: 0f,
        rejectedCount = counts?.rejected ?: 0,
        rejectedPercentage = percentages?.rejected ?: 0f,
        applications = applicationCardUiModels,
    )
}
