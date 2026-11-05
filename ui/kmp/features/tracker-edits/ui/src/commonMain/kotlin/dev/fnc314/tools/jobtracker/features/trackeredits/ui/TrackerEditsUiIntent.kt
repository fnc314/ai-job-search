package dev.fnc314.tools.jobtracker.features.trackeredits.ui

/**
 * User-initiated intents dispatched to [TrackerEditsViewModel].
 */
public sealed interface TrackerEditsUiIntent {
    /** Reloads the list of pending proposed edits from Firestore. */
    public data object Refresh : TrackerEditsUiIntent

    /**
     * Approves and merges the proposed edit into the application record.
     *
     * @property id Unique identifier of the edit to accept.
     * @property note Optional reviewer comment recorded in the audit event.
     */
    public data class ApproveEdit(public val id: String, public val note: String? = null) : TrackerEditsUiIntent

    /**
     * Rejects the proposed edit without altering the application entity.
     *
     * @property id Unique identifier of the edit to reject.
     * @property note Optional rejection justification.
     */
    public data class RejectEdit(public val id: String, public val note: String? = null) : TrackerEditsUiIntent

    /** Triggers batch transition of expired pending edits to dead-letter status. */
    public data object ExpireStaleEdits : TrackerEditsUiIntent

    /** Requests back navigation to the preceding screen. */
    public data object ClickBack : TrackerEditsUiIntent
}
