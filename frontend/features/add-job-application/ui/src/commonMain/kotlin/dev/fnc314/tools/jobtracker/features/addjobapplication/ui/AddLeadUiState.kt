package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

/**
 * Screen state model for the add lead (prospective opportunity) workflow.
 */
public sealed interface AddLeadUiState {
    /**
     * Active state where the user is entering lead details.
     *
     * @property form Current field values in the form.
     */
    public data class Editing(public val form: AddLeadFormUiModel) : AddLeadUiState

    /** State indicating lead creation is processing asynchronously. */
    public data object Submitting : AddLeadUiState

    /**
     * State indicating the lead was successfully recorded.
     *
     * @property leadId Unique identifier of the created lead record.
     */
    public data class Success(public val leadId: String) : AddLeadUiState

    /**
     * State indicating a validation failure or network submission error.
     *
     * @property message Descriptive failure explanation.
     */
    public data class Error(public val message: String) : AddLeadUiState

    /** Transient navigation state requesting return to the previous screen. */
    public data object NavigateBack : AddLeadUiState
}
