package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus

/**
 * Decoupled UI presentation model representing a single application row/card in the dashboard list.
 *
 * Formats dates, provides precomputed display strings, and isolates the UI from raw entity domain models.
 *
 * @property id Unique application identifier.
 * @property companyName Display name of the hiring organization.
 * @property roleTitle Formatted position title.
 * @property locationText Office/remote summary string.
 * @property status Current lifecycle status [TrackerStatus].
 * @property statusDisplayName Human-readable status label.
 * @property fitScore Numerical fit score (0..100) or null.
 * @property fitScoreText Pre-formatted fit score string (e.g. `"Fit: 85%"`).
 * @property formattedCreatedDate Relative or calendar creation date string.
 * @property onClickIntent UI intent dispatched upon selecting this row.
 */
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
