package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.time.Instant

public data class TrackerEdit(
    public val id: String,
    public val applicationId: String,
    public val kind: TrackerEditKind,
    public val state: TrackerEditState,
    public val trigger: Trigger,
    public val changes: List<EditChange>,
    public val unmatched: List<EditUnmatched>,
    public val sourceEmail: SourceEmail?,
    public val scope: EditScope?,
    public val confidence: Confidence,
    public val createdAt: Instant?,
    public val expiresAt: Instant?,
)

public data class EditChange(
    public val field: String,
    public val oldValue: String,
    public val newValue: String,
    public val reason: String,
)

public data class EditUnmatched(
    public val field: String,
    public val value: String,
)

public data class SourceEmail(
    public val messageId: String,
    public val from: String,
    public val subject: String,
)

public data class EditScope(
    public val collection: String,
    public val documentId: String,
)
