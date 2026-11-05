package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateLeadUseCase
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public class AddLeadViewModel(
    private val createLead: CreateLeadUseCase,
) : ViewModel() {

    private var currentForm: AddLeadFormUiModel = AddLeadFormUiModel()

    private val _uiState: MutableStateFlow<AddLeadUiState> =
        MutableStateFlow(AddLeadUiState.Editing(currentForm))
    public val uiState: StateFlow<AddLeadUiState> = _uiState.asStateFlow()

    public fun onIntent(intent: AddLeadUiIntent): Unit {
        when (intent) {
            is AddLeadUiIntent.UpdateCompany -> updateForm { it.copy(company = intent.value) }
            is AddLeadUiIntent.UpdateContact -> updateForm { it.copy(contact = intent.value) }
            is AddLeadUiIntent.UpdateRole -> updateForm { it.copy(role = intent.value) }
            is AddLeadUiIntent.UpdateChannel -> updateForm { it.copy(channel = intent.value) }
            is AddLeadUiIntent.UpdateNotes -> updateForm { it.copy(notes = intent.notes) }
            is AddLeadUiIntent.UpdateTags -> updateForm { it.copy(tags = intent.value) }
            is AddLeadUiIntent.Submit -> handleSubmit()
            is AddLeadUiIntent.ClickBack -> {
                _uiState.value = AddLeadUiState.NavigateBack
            }
            is AddLeadUiIntent.DismissError -> {
                currentForm = currentForm.copy(validationError = null)
                _uiState.value = AddLeadUiState.Editing(currentForm)
            }
        }
    }

    private fun updateForm(transform: (AddLeadFormUiModel) -> AddLeadFormUiModel): Unit {
        currentForm = transform(currentForm).copy(validationError = null)
        _uiState.value = AddLeadUiState.Editing(currentForm)
    }

    private fun handleSubmit(): Unit {
        if (currentForm.company.isBlank()) {
            val error = "Company name is required"
            currentForm = currentForm.copy(validationError = error)
            _uiState.value = AddLeadUiState.Error(error)
            return
        }

        _uiState.value = AddLeadUiState.Submitting

        viewModelScope.launch {
            try {
                val parsedTags = currentForm.tags
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val leadNotes = if (currentForm.role.isNotBlank()) {
                    persistentListOf("Role: ${currentForm.role}") + currentForm.notes
                } else {
                    currentForm.notes
                }

                val id: String = createLead(
                    CreateLeadUseCase.Params(
                        company = currentForm.company,
                        contact = currentForm.contact.takeIf { it.isNotBlank() },
                        channel = currentForm.channel.takeIf { it.isNotBlank() },
                        notes = leadNotes,
                        tags = parsedTags,
                    )
                )
                _uiState.value = AddLeadUiState.Success(id)
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to save lead"
                currentForm = currentForm.copy(validationError = errorMessage)
                _uiState.value = AddLeadUiState.Error(errorMessage)
            }
        }
    }
}
