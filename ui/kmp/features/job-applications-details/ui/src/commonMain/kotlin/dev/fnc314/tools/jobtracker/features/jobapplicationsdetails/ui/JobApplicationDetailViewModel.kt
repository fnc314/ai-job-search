package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import androidx.lifecycle.ViewModel
import dev.fnc314.tools.jobtracker.core.data.model.*
import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class DetailUiState(
    val application: JobApplication? = null,
    val company: Company? = null,
    val events: List<TrackerEvent> = emptyList(),
    val artifacts: List<TrackerArtifact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdating: Boolean = false,
)

class JobApplicationDetailViewModel : ViewModel(), KoinComponent {
    private val loadApplicationUseCase: LoadJobApplicationUseCase by inject()
    private val loadCompaniesUseCase: LoadCompaniesUseCase by inject()
    private val updateStatusUseCase: UpdateApplicationStatusUseCase by inject()
    private val recordEventUseCase: RecordEventUseCase by inject()
    private val updateFitScoreUseCase: UpdateFitScoreUseCase by inject()
    private val getSignedUrlUseCase: GetArtifactSignedUrlUseCase by inject()
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // Simple scope for synchronous calls from Compose
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    fun loadApplication(id: String) {
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

    fun updateStatus(status: TrackerStatus, date: String? = null, note: String? = null) {
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

    fun recordEvent(type: TrackerEventType, note: String? = null) {
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

    fun updateFitScore(score: Int) {
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

    suspend fun getArtifactSignedUrl(path: String): String? {
        return try {
            getSignedUrlUseCase(GetArtifactSignedUrlUseCase.Params(path = path))
        } catch (e: Exception) {
            null
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
