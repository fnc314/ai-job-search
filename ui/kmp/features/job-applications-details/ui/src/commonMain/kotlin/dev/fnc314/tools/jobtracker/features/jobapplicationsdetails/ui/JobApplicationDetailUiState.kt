package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

public sealed interface JobApplicationDetailUiState {
    public data object Loading : JobApplicationDetailUiState
    public data class Content(
        public val model: JobApplicationDetailUiModel,
        public val isUpdating: Boolean,
    ) : JobApplicationDetailUiState
    public data class Error(
        public val message: String,
    ) : JobApplicationDetailUiState
    public data object NavigateBack : JobApplicationDetailUiState
}
