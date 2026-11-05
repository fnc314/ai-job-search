package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

public sealed interface AddLeadUiState {
    public data class Editing(public val form: AddLeadFormUiModel) : AddLeadUiState
    public data object Submitting : AddLeadUiState
    public data class Success(public val leadId: String) : AddLeadUiState
    public data class Error(public val message: String) : AddLeadUiState
    public data object NavigateBack : AddLeadUiState
}
