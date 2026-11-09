package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import kotlinx.collections.immutable.ImmutableList

/**
 * Screen state model for the proposed edits review dashboard.
 */
public sealed interface TrackerEditsUiState {
    /** State indicating pending edits are being retrieved from Firestore. */
    public data object Loading : TrackerEditsUiState

    /**
     * Active state rendering pending candidate edits requiring user approval or rejection.
     *
     * @property edits Immutable list of proposed edits formatted for presentation.
     * @property isActionInProgress Whether an approval, rejection, or expiration mutation is active.
     */
    public data class Content(
        public val edits: ImmutableList<TrackerEditItemUiModel>,
        public val isActionInProgress: Boolean = false,
    ) : TrackerEditsUiState

    /**
     * State indicating a query or mutation operation failed.
     *
     * @property message Descriptive failure explanation.
     */
    public data class Error(public val message: String) : TrackerEditsUiState

    /** Transient navigation state requesting return to the previous screen. */
    public data object NavigateBack : TrackerEditsUiState
}
