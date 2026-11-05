package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

public sealed interface AddJobApplicationUiState {
    public data class Editing(public val form: AddJobApplicationFormUiModel) : AddJobApplicationUiState
    public data object Submitting : AddJobApplicationUiState
    public data class Success(public val applicationId: String) : AddJobApplicationUiState
    public data class Error(public val message: String) : AddJobApplicationUiState
    public data object NavigateBack : AddJobApplicationUiState
}
