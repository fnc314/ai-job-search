package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui

/**
 * Reactive UI state representations for the application detail inspection screen.
 */
public sealed interface JobApplicationDetailUiState {

    /** State rendered while loading application details and timeline events. */
    public data object Loading : JobApplicationDetailUiState

    /**
     * Active content state rendering the complete application profile.
     *
     * @property model The decoupled UI presentation model [JobApplicationDetailUiModel].
     * @property isUpdating True while asynchronous status mutations or event recordings are executing.
     */
    public data class Content(
        public val model: JobApplicationDetailUiModel,
        public val isUpdating: Boolean,
    ) : JobApplicationDetailUiState

    /**
     * Error state rendered when fetching or updating application details fails.
     *
     * @property message Descriptive failure message for user display.
     */
    public data class Error(
        public val message: String,
    ) : JobApplicationDetailUiState

    /** Navigation side-effect state navigating back to the list dashboard. */
    public data object NavigateBack : JobApplicationDetailUiState
}
