package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class TrackerEditChangeUiModel(
    public val field: String,
    public val changeDescription: String,
    public val reason: String?,
)

@Immutable
public data class TrackerEditSourceEmailUiModel(
    public val from: String,
    public val subject: String,
)

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
