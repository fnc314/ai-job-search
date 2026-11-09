package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

/**
 * Screen state model for the add job application workflow.
 */
public sealed interface AddJobApplicationUiState {
    /**
     * Active state where the user is entering or editing application details.
     *
     * @property form Current field values in the form.
     */
    public data class Editing(public val form: AddJobApplicationFormUiModel) : AddJobApplicationUiState

    /** State indicating an asynchronous creation request is in-flight. */
    public data object Submitting : AddJobApplicationUiState

    /**
     * State indicating application creation completed successfully.
     *
     * @property applicationId Unique identifier assigned to the newly created application record.
     */
    public data class Success(public val applicationId: String) : AddJobApplicationUiState

    /**
     * State indicating a validation failure or network submission error.
     *
     * @property message Descriptive failure explanation.
     */
    public data class Error(public val message: String) : AddJobApplicationUiState

    /** Transient navigation state requesting return to the previous screen. */
    public data object NavigateBack : AddJobApplicationUiState
}
