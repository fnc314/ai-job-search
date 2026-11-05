package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * Presentation model capturing a specific field mutation within a proposed edit.
 *
 * @property field Targeted entity property name.
 * @property changeDescription Human-readable summary formatted as `oldValue → newValue`.
 * @property reason Explanatory rationale or automated heuristic justification.
 */
@Immutable
public data class TrackerEditChangeUiModel(
    public val field: String,
    public val changeDescription: String,
    public val reason: String?,
)

/**
 * Presentation model displaying metadata for the email message that triggered this proposed edit.
 *
 * @property from Sender address or display name.
 * @property subject Email subject line.
 */
@Immutable
public data class TrackerEditSourceEmailUiModel(
    public val from: String,
    public val subject: String,
)

/**
 * Presentation model representing an individual proposed edit card.
 *
 * @property id Unique identifier of the pending edit proposal.
 * @property applicationId Unique Firestore identifier of the parent job application.
 * @property kindDisplayName Human-readable name of the edit category ([dev.fnc314.tools.jobtracker.core.data.model.TrackerEditKind]).
 * @property confidenceDisplayName Confidence rating label ([dev.fnc314.tools.jobtracker.core.data.model.Confidence]).
 * @property changeSummary Concise overview string summarizing all affected fields.
 * @property changes List of detailed field mutations in this proposal.
 * @property timestamp Human-readable formatted creation or expiration time.
 * @property sourceEmail Originating email communication metadata, if applicable.
 * @property approveIntent Pre-configured [TrackerEditsUiIntent.ApproveEdit] intent.
 * @property rejectIntent Pre-configured [TrackerEditsUiIntent.RejectEdit] intent.
 */
@Immutable
public data class TrackerEditItemUiModel(
    public val id: String,
    public val applicationId: String,
    public val kindDisplayName: String,
    public val confidenceDisplayName: String,
    public val changeSummary: String,
    public val changes: ImmutableList<TrackerEditChangeUiModel>,
    public val timestamp: String,
    public val sourceEmail: TrackerEditSourceEmailUiModel?,
    public val approveIntent: TrackerEditsUiIntent.ApproveEdit,
    public val rejectIntent: TrackerEditsUiIntent.RejectEdit,
)

internal fun TrackerEdit.toUiModel(): TrackerEditItemUiModel {
    val uiChanges = changes.map { change ->
        TrackerEditChangeUiModel(
            field = change.field,
            changeDescription = "${change.oldValue} → ${change.newValue}",
            reason = change.reason.takeIf { it.isNotBlank() },
        )
    }.toPersistentList()

    val formattedSummary = if (changes.isNotEmpty()) {
        changes.joinToString(separator = "; ") { "${it.field}: ${it.oldValue} → ${it.newValue}" }
    } else {
        "No changes specified"
    }

    val formattedTimestamp = createdAt?.toString() ?: expiresAt?.toString() ?: ""

    val emailModel = sourceEmail?.let {
        TrackerEditSourceEmailUiModel(
            from = it.from,
            subject = it.subject,
        )
    }

    return TrackerEditItemUiModel(
        id = id,
        applicationId = applicationId,
        kindDisplayName = kind.displayName(),
        confidenceDisplayName = confidence.displayName(),
        changeSummary = formattedSummary,
        changes = uiChanges,
        timestamp = formattedTimestamp,
        sourceEmail = emailModel,
        approveIntent = TrackerEditsUiIntent.ApproveEdit(id = id),
        rejectIntent = TrackerEditsUiIntent.RejectEdit(id = id),
    )
}
