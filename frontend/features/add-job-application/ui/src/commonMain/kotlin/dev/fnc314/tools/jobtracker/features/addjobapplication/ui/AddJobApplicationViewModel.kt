package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Presentation ViewModel managing form input, field validation, and application creation.
 *
 * @param createApplication Use case responsible for constructing and persisting new application entities.
 */
public class AddJobApplicationViewModel(
    private val createApplication: CreateJobApplicationUseCase,
) : ViewModel() {

    private var currentForm: AddJobApplicationFormUiModel = AddJobApplicationFormUiModel()

    private val _uiState: MutableStateFlow<AddJobApplicationUiState> =
        MutableStateFlow(AddJobApplicationUiState.Editing(currentForm))

    /**
     * Observable stream of [AddJobApplicationUiState] reflecting form edits, loading states, and outcomes.
     */
    public val uiState: StateFlow<AddJobApplicationUiState> = _uiState.asStateFlow()

    /**
     * Dispatches a user intent to be processed by this ViewModel.
     *
     * @param intent The [AddJobApplicationUiIntent] to process.
     */
    public fun onIntent(intent: AddJobApplicationUiIntent): Unit {
        when (intent) {
            is AddJobApplicationUiIntent.UpdateCompany -> updateForm { it.copy(company = intent.value) }
            is AddJobApplicationUiIntent.UpdateRole -> updateForm { it.copy(role = intent.value) }
            is AddJobApplicationUiIntent.UpdateSector -> updateForm { it.copy(sector = intent.value) }
            is AddJobApplicationUiIntent.UpdateRoleType -> updateForm { it.copy(roleType = intent.value) }
            is AddJobApplicationUiIntent.UpdateWorkMode -> updateForm { it.copy(workMode = intent.mode) }
            is AddJobApplicationUiIntent.UpdateChannel -> updateForm { it.copy(channel = intent.channel) }
            is AddJobApplicationUiIntent.UpdateStatus -> updateForm { it.copy(status = intent.status) }
            is AddJobApplicationUiIntent.UpdateJobUrl -> updateForm { it.copy(jobUrl = intent.value) }
            is AddJobApplicationUiIntent.UpdateJobApplicationUrl -> updateForm { it.copy(jobApplicationUrl = intent.value) }
            is AddJobApplicationUiIntent.UpdateJobId -> updateForm { it.copy(jobId = intent.value) }
            is AddJobApplicationUiIntent.UpdateLocation -> updateForm { it.copy(location = intent.value) }
            is AddJobApplicationUiIntent.UpdateNotes -> updateForm { it.copy(notes = intent.notes) }
            is AddJobApplicationUiIntent.UpdateTags -> updateForm { it.copy(tags = intent.value) }
            is AddJobApplicationUiIntent.Submit -> handleSubmit()
            is AddJobApplicationUiIntent.ClickBack -> {
                _uiState.value = AddJobApplicationUiState.NavigateBack
            }
            is AddJobApplicationUiIntent.DismissError -> {
                currentForm = currentForm.copy(validationError = null)
                _uiState.value = AddJobApplicationUiState.Editing(currentForm)
            }
        }
    }

    private fun updateForm(transform: (AddJobApplicationFormUiModel) -> AddJobApplicationFormUiModel): Unit {
        currentForm = transform(currentForm).copy(validationError = null)
        _uiState.value = AddJobApplicationUiState.Editing(currentForm)
    }

    private fun handleSubmit(): Unit {
        if (currentForm.company.isBlank()) {
            val error = "Company name is required"
            currentForm = currentForm.copy(validationError = error)
            _uiState.value = AddJobApplicationUiState.Error(error)
            return
        }
        if (currentForm.role.isBlank()) {
            val error = "Role is required"
            currentForm = currentForm.copy(validationError = error)
            _uiState.value = AddJobApplicationUiState.Error(error)
            return
        }

        _uiState.value = AddJobApplicationUiState.Submitting

        viewModelScope.launch {
            try {
                val now = Clock.System.now()
                val parsedTags = currentForm.tags
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val notesString = if (currentForm.notes.isNotEmpty()) {
                    currentForm.notes.joinToString("\n")
                } else {
                    null
                }

                val app: JobApplication = Factory.createJobApplication(
                    id = now.toString(),
                    company = currentForm.company,
                    role = currentForm.role.takeIf { it.isNotBlank() },
                    location = currentForm.location.takeIf { it.isNotBlank() },
                    status = currentForm.status,
                    channel = currentForm.channel,
                    jobId = currentForm.jobId.takeIf { it.isNotBlank() },
                    jobUrl = currentForm.jobUrl.takeIf { it.isNotBlank() },
                    jobApplicationUrl = currentForm.jobApplicationUrl.takeIf { it.isNotBlank() },
                    tags = parsedTags,
                    notesRendered = notesString,
                    createdAt = now,
                    updatedAt = now,
                    submittedAt = now,
                )

                val id = createApplication(CreateJobApplicationUseCase.Params(app))
                _uiState.value = AddJobApplicationUiState.Success(id)
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to save application"
                currentForm = currentForm.copy(validationError = errorMessage)
                _uiState.value = AddJobApplicationUiState.Error(errorMessage)
            }
        }
    }
}
