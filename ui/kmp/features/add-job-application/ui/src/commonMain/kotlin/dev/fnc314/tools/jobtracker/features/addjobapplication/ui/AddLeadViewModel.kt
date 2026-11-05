package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateLeadUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public data class AddLeadFormState(
    val company: String = "",
    val contact: String = "",
    val channel: String = "",
    val notes: ImmutableList<String> = persistentListOf(),
    val tags: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

public class AddLeadViewModel(
    private val createLead: CreateLeadUseCase,
) : ViewModel() {

    private val _formState = MutableStateFlow(AddLeadFormState())
    public val formState: StateFlow<AddLeadFormState> = _formState.asStateFlow()

    public fun updateCompany(value: String) {
        _formState.update { it.copy(company = value, error = null) }
    }

    public fun updateContact(value: String) {
        _formState.update { it.copy(contact = value, error = null) }
    }

    public fun updateChannel(value: String) {
        _formState.update { it.copy(channel = value, error = null) }
    }

    public fun updateNotes(value: ImmutableList<String>) {
        _formState.update { it.copy(notes = value, error = null) }
    }

    public fun updateTags(value: String) {
        _formState.update { it.copy(tags = value, error = null) }
    }

    public fun validate(): Boolean {
        val state = _formState.value
        if (state.company.isBlank()) {
            _formState.update { it.copy(error = "Company name is required") }
            return false
        }
        return true
    }

    public fun submit(onSuccess: (String) -> Unit) {
        if (!validate()) return

        val state = _formState.value
        _formState.update { it.copy(submitting = true, error = null) }

        viewModelScope.launch {
            try {
                val parsedTags = state.tags
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val id = createLead(
                    CreateLeadUseCase.Params(
                        company = state.company,
                        contact = state.contact.takeIf { it.isNotBlank() },
                        channel = state.channel.takeIf { it.isNotBlank() },
                        notes = state.notes,
                        tags = parsedTags,
                    )
                )
                _formState.update { it.copy(submitting = false) }
                onSuccess(id)
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        submitting = false,
                        error = e.message ?: "Failed to save lead"
                    )
                }
            }
        }
    }

    public fun reset() {
        _formState.value = AddLeadFormState()
    }
}
