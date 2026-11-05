package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

public data class AddApplicationFormState(
    val company: String = "",
    val role: String = "",
    val sector: String = "",
    val roleType: String = "",
    val workMode: WorkMode = WorkMode.UNSPECIFIED,
    val channel: ApplicationChannel = ApplicationChannel.UNSPECIFIED,
    val status: TrackerStatus = TrackerStatus.APPLIED,
    val jobUrl: String = "",
    val jobApplicationUrl: String = "",
    val jobId: String = "",
    val location: String = "",
    val notes: ImmutableList<String> = persistentListOf(),
    val tags: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

public class AddJobApplicationViewModel(
    private val createApplication: CreateJobApplicationUseCase,
) : ViewModel() {

    private val _formState = MutableStateFlow(AddApplicationFormState())
    public val formState: StateFlow<AddApplicationFormState> = _formState.asStateFlow()

    public fun updateCompany(value: String) {
        _formState.update { it.copy(company = value, error = null) }
    }

    public fun updateRole(value: String) {
        _formState.update { it.copy(role = value, error = null) }
    }

    public fun updateSector(value: String) {
        _formState.update { it.copy(sector = value, error = null) }
    }

    public fun updateRoleType(value: String) {
        _formState.update { it.copy(roleType = value, error = null) }
    }

    public fun updateWorkMode(value: WorkMode) {
        _formState.update { it.copy(workMode = value, error = null) }
    }

    public fun updateChannel(value: ApplicationChannel) {
        _formState.update { it.copy(channel = value, error = null) }
    }

    public fun updateStatus(value: TrackerStatus) {
        _formState.update { it.copy(status = value, error = null) }
    }

    public fun updateJobUrl(value: String) {
        _formState.update { it.copy(jobUrl = value, error = null) }
    }

    public fun updateJobApplicationUrl(value: String) {
        _formState.update { it.copy(jobApplicationUrl = value, error = null) }
    }

    public fun updateJobId(value: String) {
        _formState.update { it.copy(jobId = value, error = null) }
    }

    public fun updateLocation(value: String) {
        _formState.update { it.copy(location = value, error = null) }
    }

    public fun updateNotes(value: ImmutableList<String>) {
        _formState.update { it.copy(notes = value, error = null) }
    }

    public fun updateTags(value: String) {
        _formState.update { it.copy(tags = value, error = null) }
    }

    public fun validate(): Boolean {
        val state = _formState.value
        val companyValid = state.company.isNotBlank()
        val roleValid = state.role.isNotBlank()

        if (!companyValid) {
            _formState.update { it.copy(error = "Company name is required") }
            return false
        }
        if (!roleValid) {
            _formState.update { it.copy(error = "Role is required") }
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
                val now = Clock.System.now()
                val parsedTags = state.tags
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val notesString = if (state.notes.isNotEmpty()) {
                    state.notes.joinToString("\n")
                } else null

                val app = Factory.createJobApplication(
                    id = now.toString(),
                    company = state.company,
                    role = state.role.takeIf { it.isNotBlank() },
                    location = state.location.takeIf { it.isNotBlank() },
                    status = state.status,
                    channel = state.channel,
                    jobId = state.jobId.takeIf { it.isNotBlank() },
                    jobUrl = state.jobUrl.takeIf { it.isNotBlank() },
                    jobApplicationUrl = state.jobApplicationUrl.takeIf { it.isNotBlank() },
                    tags = parsedTags,
                    notesRendered = notesString,
                    createdAt = now,
                    updatedAt = now,
                )

                val id = createApplication(CreateJobApplicationUseCase.Params(app))
                _formState.update { it.copy(submitting = false) }
                onSuccess(id)
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        submitting = false,
                        error = e.message ?: "Failed to save application"
                    )
                }
            }
        }
    }

    public fun reset() {
        _formState.value = AddApplicationFormState()
    }
}
