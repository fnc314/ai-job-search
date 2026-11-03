package dev.fnc314.tools.jobtracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.data.model.WorkMode
import dev.fnc314.tools.jobtracker.di.Factory
import dev.fnc314.tools.jobtracker.domain.usecase.CreateJobApplicationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class AddApplicationFormState(
    val company: String = "",
    val role: String = "",
    val sector: String = "",
    val roleType: String = "",
    val workMode: WorkMode = WorkMode.UNSPECIFIED,
    val channel: ApplicationChannel = ApplicationChannel.UNSPECIFIED,
    val status: TrackerStatus = TrackerStatus.APPLIED,
    val jobUrl: String = "",
    val jobId: String = "",
    val location: String = "",
    val notes: String = "",
    val tags: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

class AddJobApplicationViewModel(
    private val createApplication: CreateJobApplicationUseCase,
) : ViewModel() {

    private val _formState = MutableStateFlow(AddApplicationFormState())
    val formState: StateFlow<AddApplicationFormState> = _formState.asStateFlow()

    fun updateCompany(value: String) {
        _formState.update { it.copy(company = value, error = null) }
    }

    fun updateRole(value: String) {
        _formState.update { it.copy(role = value, error = null) }
    }

    fun updateSector(value: String) {
        _formState.update { it.copy(sector = value, error = null) }
    }

    fun updateRoleType(value: String) {
        _formState.update { it.copy(roleType = value, error = null) }
    }

    fun updateWorkMode(value: WorkMode) {
        _formState.update { it.copy(workMode = value, error = null) }
    }

    fun updateChannel(value: ApplicationChannel) {
        _formState.update { it.copy(channel = value, error = null) }
    }

    fun updateStatus(value: TrackerStatus) {
        _formState.update { it.copy(status = value, error = null) }
    }

    fun updateJobUrl(value: String) {
        _formState.update { it.copy(jobUrl = value, error = null) }
    }

    fun updateJobId(value: String) {
        _formState.update { it.copy(jobId = value, error = null) }
    }

    fun updateLocation(value: String) {
        _formState.update { it.copy(location = value, error = null) }
    }

    fun updateNotes(value: String) {
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
            state.status == TrackerStatus.UNSPECIFIED -> {
                _formState.update { it.copy(error = "Status is required") }
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
                val tagsList = state.tags
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val app = Factory.createJobApplication(
                    id = Clock.System.now().toString(),
                    company = state.company.trim(),
                    role = state.role.takeIf { it.isNotBlank() },
                    status = state.status,
                    channel = state.channel,
                    jobId = state.jobId.takeIf { it.isNotBlank() },
                    jobUrl = state.jobUrl.takeIf { it.isNotBlank() },
                    location = state.location.takeIf { it.isNotBlank() },
                    description = state.roleType.takeIf { it.isNotBlank() },
                    tags = tagsList,
                    notesRendered = state.notes.takeIf { it.isNotBlank() },
                    locationDetail = if (state.workMode != WorkMode.UNSPECIFIED || state.location.isNotBlank()) {
                        Factory.createLocation(
                            city = state.location.takeIf { it.isNotBlank() },
                            workMode = state.workMode,
                        )
                    } else null,
                )

                createApplication(CreateJobApplicationUseCase.Params(app))
                onSuccess()
            } catch (e: Exception) {
                _formState.update { it.copy(submitting = false, error = e.message ?: "Failed to create application") }
            }
        }
    }
}
