package dev.fnc314.tools.jobtracker.data.model

import kotlin.time.Instant

data class TrackerEdit(
    val id: String,
    val applicationId: String,
    val kind: TrackerEditKind,
    val state: TrackerEditState,
    val trigger: Trigger,
    val changes: List<EditChange>,
    val unmatched: List<EditUnmatched>,
    val sourceEmail: SourceEmail?,
    val scope: EditScope?,
    val confidence: Confidence,
    val createdAt: Instant?,
    val expiresAt: Instant?,
)

data class EditChange(
    val field: String,
    val oldValue: String,
    val newValue: String,
    val reason: String,
)

data class EditUnmatched(
    val field: String,
    val value: String,
)

data class SourceEmail(
    val messageId: String,
    val from: String,
    val subject: String,
)

data class EditScope(
    val collection: String,
    val documentId: String,
)
