package dev.fnc314.tools.jobtracker.ui.lead

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.domain.usecase.CreateLeadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddLeadFormState(
    val company: String = "",
    val contact: String = "",
    val channel: String = "",
    val notes: List<String> = emptyList(),
    val tags: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

class AddLeadViewModel(
    private val createLead: CreateLeadUseCase,
) : ViewModel() {

    private val _formState = MutableStateFlow(AddLeadFormState())
    val formState: StateFlow<AddLeadFormState> = _formState.asStateFlow()

    fun updateCompany(value: String) {
        _formState.update { it.copy(company = value, error = null) }
    }

    fun updateContact(value: String) {
        _formState.update { it.copy(contact = value, error = null) }
    }

    fun updateChannel(value: String) {
        _formState.update { it.copy(channel = value, error = null) }
    }

    fun updateNotes(value: List<String>) {
        _formState.update { it.copy(notes = value, error = null) }
    }

    fun updateTags(value: String) {
        _formState.update { it.copy(tags = value, error = null) }
    }

    fun validate(): Boolean {
        val state = _formState.value
        return when {
            state.company.isBlank() -> {
                _formState.update { it.copy(error = "Company is required") }
                false
            }
            else -> {
                _formState.update { it.copy(error = null) }
                true
            }
        }
    }

    fun submit(onSuccess: () -> Unit) {
        if (!validate()) return

        val state = _formState.value
        _formState.update { it.copy(submitting = true, error = null) }

        viewModelScope.launch {
            try {
                val notesList = state.notes
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                val tagsList = state.tags
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                createLead(CreateLeadUseCase.Params(
                    company = state.company.trim(),
                    contact = state.contact.takeIf { it.isNotBlank() },
                    channel = state.channel.takeIf { it.isNotBlank() },
                    notes = notesList,
                    tags = tagsList,
                ))
                onSuccess()
            } catch (e: Exception) {
                _formState.update { it.copy(submitting = false, error = e.message ?: "Failed to create lead") }
            }
        }
    }
}
