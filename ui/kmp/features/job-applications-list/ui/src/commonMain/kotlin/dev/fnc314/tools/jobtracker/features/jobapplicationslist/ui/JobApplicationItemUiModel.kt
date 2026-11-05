package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus

@Immutable
public data class JobApplicationItemUiModel(
    public val id: String,
    public val companyName: String,
    public val roleTitle: String,
    public val locationText: String?,
    public val status: TrackerStatus,
    public val statusDisplayName: String,
    public val fitScore: Int?,
    public val fitScoreText: String?,
    public val formattedCreatedDate: String,
    public val onClickIntent: JobApplicationsListUiIntent,
)
