package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import kotlinx.collections.immutable.ImmutableList

public sealed interface TrackerEditsUiState {
    public data object Loading : TrackerEditsUiState
    public data class Content(
        public val edits: ImmutableList<TrackerEditItemUiModel>,
        public val isActionInProgress: Boolean = false,
    ) : TrackerEditsUiState
    public data class Error(public val message: String) : TrackerEditsUiState
    public data object NavigateBack : TrackerEditsUiState
}
