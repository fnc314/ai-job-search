package dev.fnc314.tools.jobtracker.features.trackeredits.ui

public sealed interface TrackerEditsUiIntent {
    public data object Refresh : TrackerEditsUiIntent
    public data class ApproveEdit(public val id: String, public val note: String? = null) : TrackerEditsUiIntent
    public data class RejectEdit(public val id: String, public val note: String? = null) : TrackerEditsUiIntent
    public data object ExpireStaleEdits : TrackerEditsUiIntent
    public data object ClickBack : TrackerEditsUiIntent
}
