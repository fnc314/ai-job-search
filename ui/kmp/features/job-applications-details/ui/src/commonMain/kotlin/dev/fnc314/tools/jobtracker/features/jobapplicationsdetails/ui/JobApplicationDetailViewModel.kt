package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import androidx.lifecycle.ViewModel
import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.Confidence
import dev.fnc314.tools.jobtracker.core.data.model.DerivedFrom
import dev.fnc314.tools.jobtracker.core.data.model.EventActor
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.GetArtifactSignedUrlUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.LoadCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.LoadJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.RecordEventUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateApplicationStatusUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateFitScoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.Uuid

public data class DetailUiState(
    val application: JobApplication? = null,
    val company: Company? = null,
    val events: List<TrackerEvent> = emptyList(),
    val artifacts: List<TrackerArtifact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdating: Boolean = false,
)

public class JobApplicationDetailViewModel(
    private val loadApplicationUseCase: LoadJobApplicationUseCase,
    private val loadCompaniesUseCase: LoadCompaniesUseCase,
    private val updateStatusUseCase: UpdateApplicationStatusUseCase,
    private val recordEventUseCase: RecordEventUseCase,
    private val updateFitScoreUseCase: UpdateFitScoreUseCase,
    private val getSignedUrlUseCase: GetArtifactSignedUrlUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailUiState())
    public val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // Simple scope for synchronous calls from Compose
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    public fun loadApplication(id: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val app = loadApplicationUseCase(LoadJobApplicationUseCase.Params(id = id))
                val companies = loadCompaniesUseCase(LoadCompaniesUseCase.Params(forceRefresh = false))
                val company = companies.find { it.name.equals(app?.company ?: "", ignoreCase = true) }
                _uiState.update {
                    it.copy(
                        application = app,
                        company = company,
                        isLoading = false,
                        events = emptyList(),
                        artifacts = emptyList(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    public fun updateStatus(status: TrackerStatus, date: String? = null, note: String? = null) {
        val app = _uiState.value.application ?: return
        scope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                val updated = updateStatusUseCase(UpdateApplicationStatusUseCase.Params(
                    id = app.id,
                    status = status,
                    date = date,
                    note = note,
                ))
                _uiState.update { it.copy(application = updated, isUpdating = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdating = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    public fun recordEvent(type: TrackerEventType, note: String? = null) {
        val app = _uiState.value.application ?: return
        val event = Factory.createTrackerEvent(
            id = Uuid.random().toString(),
            applicationId = app.id,
            type = type,
            statusFrom = app.status,
            statusTo = null,
            note = note,
            emailMessageId = null,
            actor = EventActor.USER,
            derivedFrom = DerivedFrom.MANUAL,
            confidence = Confidence.HIGH,
            createdAt = Clock.System.now(),
        )
        scope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                recordEventUseCase(RecordEventUseCase.Params(event = event))
                _uiState.update { it.copy(isUpdating = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdating = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    public fun updateFitScore(score: Int) {
        val app = _uiState.value.application ?: return
        scope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                val updated = updateFitScoreUseCase(UpdateFitScoreUseCase.Params(id = app.id, score = score))
                _uiState.update { it.copy(application = updated, isUpdating = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdating = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    public suspend fun getArtifactSignedUrl(path: String): String? {
        return try {
            getSignedUrlUseCase(GetArtifactSignedUrlUseCase.Params(path = path))
        } catch (e: Exception) {
            null
        }
    }

    public fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
