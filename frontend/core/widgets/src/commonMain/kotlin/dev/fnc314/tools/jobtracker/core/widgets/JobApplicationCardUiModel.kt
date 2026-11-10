package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import fnc314jobsearchtracker.core_widgets.generated.resources.Res
import fnc314jobsearchtracker.core_widgets.generated.resources.card_not_available
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

/**
 * Decoupled UI presentation model representing a single job application card.
 *
 * @property id Unique application identifier.
 * @property companyName Display name of the hiring organization.
 * @property roleTitle Formatted position title.
 * @property locationText Office/remote summary string.
 * @property status Current lifecycle status [TrackerStatus].
 * @property statusDisplayName Human-readable status label.
 * @property fitScore Numerical fit score (0..100) or null.
 * @property fitScoreText Pre-formatted fit score string (e.g. `"Fit 85"`).
 * @property createdAt Record creation timestamp.
 * @property updatedAt Last modification timestamp.
 * @property firstActivityAt First recorded activity (application date).
 * @property isExpanded Whether the compact card is expanded.
 * @property isAutoSynced Whether this application was auto-synced via email.
 * @property emailCount Number of linked email communications.
 * @property logoUrl Direct company logo URL if known.
 */
@Immutable
public data class JobApplicationCardUiModel(
    public val id: String,
    public val companyName: String,
    public val roleTitle: String,
    public val locationText: String? = null,
    public val status: TrackerStatus = TrackerStatus.Known.UNSPECIFIED,
    public val statusDisplayName: String = status.displayName,
    public val fitScore: Int? = null,
    public val fitScoreText: String? = null,
    public val createdAt: Instant = Instant.DISTANT_PAST,
    public val updatedAt: Instant = Instant.DISTANT_PAST,
    public val firstActivityAt: Instant = Instant.DISTANT_PAST,
    public val isExpanded: Boolean = false,
    public val isAutoSynced: Boolean = false,
    public val emailCount: Int = 0,
    public val logoUrl: String? = null,
) {
    /** Single uppercase alphanumeric character representing the company monogram, or `"?"` when blank. */
    public val companyInitial: String
        get() = companyName.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"

    /** Whether [status] indicates the application has already been submitted to the employer. */
    public val hasBeenSubmitted: Boolean
        get() = when (status) {
            TrackerStatus.Known.NONE,
            TrackerStatus.Known.UNSPECIFIED,
            TrackerStatus.Known.LEAD,
            TrackerStatus.Known.DRAFTING -> false
            else -> true
        }

    /** Creation date as `YYYY-MM-DD`, or `null` when unknown. */
    public fun createdDateOrNull(): String? = createdAt.dateOrNull()

    /** Creation date as `YYYY-MM-DD`, or [backup] when unknown. */
    public fun createdDateDisplay(backup: String): String = createdDateOrNull() ?: backup

    /** Application date as `YYYY-MM-DD`, or `null` when not yet applied / unknown. */
    public fun appliedDateOrNull(): String? =
        firstActivityAt.dateOrNull() ?: if (hasBeenSubmitted) createdAt.dateOrNull() else null

    /** Application date as `YYYY-MM-DD`, or [backup] when unknown. */
    public fun appliedDateDisplay(backup: String): String = appliedDateOrNull() ?: backup

    /** Localized relative last-update text (falls back to creation), or `null` when unknown. */
    @Composable
    public fun updatedDateOrNull(): String? {
        val target = if (updatedAt != Instant.DISTANT_PAST) updatedAt else createdAt
        return formatRelativeInstant(target)
    }

    /** Localized relative last-update text (falls back to creation), or [backup] when unknown. */
    @Composable
    public fun updatedDateDisplay(backup: String = stringResource(Res.string.card_not_available)): String =
        updatedDateOrNull() ?: backup

    /** Fit score as text, or `null` when unknown. */
    public fun fitScoreOrNull(): String? = fitScore?.toString()

    /** Fit score as text, or [backup] when unknown. */
    public fun fitScoreDisplay(backup: String): String = fitScoreOrNull() ?: backup

    private fun Instant.dateOrNull(): String? =
        takeUnless { it == Instant.DISTANT_PAST }?.toString()?.substringBefore('T')
}
