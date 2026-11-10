package dev.fnc314.tools.jobtracker.features.companieslist.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.Company

/**
 * Decoupled UI presentation model for rendering an employer tile/card.
 */
@Immutable
public data class CompanyCardUiModel(
    public val id: String,
    public val name: String,
    public val logoUrl: String? = null,
    public val website: String? = null,
    public val industry: String? = null,
    public val totalApplications: Int = 0,
    public val submittedApplications: Int = 0,
    public val draftApplications: Int = 0,
    public val interviewApplications: Int = 0,
    public val offerApplications: Int = 0,
    public val rejectedApplications: Int = 0,
    public val draftPercentage: Float = 0f,
    public val submittedPercentage: Float = 0f,
    public val interviewPercentage: Float = 0f,
    public val offerPercentage: Float = 0f,
    public val rejectedPercentage: Float = 0f,
    public val averageFitScore: Double = 0.0,
    public val averageFitScoreText: String = if (averageFitScore > 0.0) "${averageFitScore.toInt()}" else "—",
    public val salaryRangeDisplay: String = "",
    public val primaryChannelDisplay: String = "",
    public val locationsText: String = "",
    public val isExpanded: Boolean = false,
) {
    /** Initial monogram letter. */
    public val companyInitial: String
        get() = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
}

/**
 * Maps a [Company] domain entity into a presentation [CompanyCardUiModel].
 */
public fun Company.toUiModel(isExpanded: Boolean = false): CompanyCardUiModel {
    val m = metrics
    val counts = m?.counts
    val percentages = m?.percentages

    return CompanyCardUiModel(
        id = id,
        name = name,
        logoUrl = logoUrl,
        website = website,
        industry = industry,
        totalApplications = counts?.total ?: 0,
        submittedApplications = counts?.submitted ?: 0,
        draftApplications = counts?.draft ?: 0,
        interviewApplications = counts?.interview ?: 0,
        offerApplications = counts?.offer ?: 0,
        rejectedApplications = counts?.rejected ?: 0,
        draftPercentage = percentages?.draft ?: 0f,
        submittedPercentage = percentages?.submitted ?: 0f,
        interviewPercentage = percentages?.interview ?: 0f,
        offerPercentage = percentages?.offer ?: 0f,
        rejectedPercentage = percentages?.rejected ?: 0f,
        averageFitScore = m?.averageFitScore ?: 0.0,
        salaryRangeDisplay = m?.salaryRange?.display.orEmpty(),
        primaryChannelDisplay = m?.primaryChannel?.displayName().orEmpty(),
        locationsText = m?.locations?.joinToString(", ").orEmpty(),
        isExpanded = isExpanded,
    )
}
