package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.GetArtifactSignedUrlUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.RecordEventUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.UpdateApplicationStatusUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.UpdateFitScoreUseCase
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Presentation ViewModel driving the application detail screen.
 *
 * Coordinates loading comprehensive record information, executing status and fit-score updates,
 * recording timeline events, and requesting signed download URLs for candidate artifacts.
 *
 * @param loadApplicationUseCase Use case fetching a specific application record.
 * @param loadCompaniesUseCase Use case loading company metadata and aliases.
 * @param updateStatusUseCase Use case mutating the application lifecycle status.
 * @param recordEventUseCase Use case appending a new timeline event.
 * @param updateFitScoreUseCase Use case adjusting the candidate fit score and confidence.
 * @param getSignedUrlUseCase Use case resolving download URLs for stored artifacts.
 */
public class JobApplicationDetailViewModel(
    private val loadApplicationUseCase: LoadJobApplicationUseCase,
    private val loadCompaniesUseCase: LoadCompaniesUseCase,
    private val updateStatusUseCase: UpdateApplicationStatusUseCase,
    private val recordEventUseCase: RecordEventUseCase,
    private val updateFitScoreUseCase: UpdateFitScoreUseCase,
    private val getSignedUrlUseCase: GetArtifactSignedUrlUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<JobApplicationDetailUiState>(JobApplicationDetailUiState.Loading)

    /**
     * Observable stream of [JobApplicationDetailUiState] reflecting the loaded detail record,
     * transient update indicators, or error states.
     */
    public val uiState: StateFlow<JobApplicationDetailUiState> = _uiState.asStateFlow()

    private var currentApplication: JobApplication? = null
    private var currentCompany: Company? = null
    private var currentEvents: List<TrackerEvent> = emptyList()
    private var currentArtifacts: List<TrackerArtifact> = emptyList()

    /**
     * Dispatches a user intent to be processed by this ViewModel.
     *
     * @param intent The [JobApplicationDetailUiIntent] to process.
     */
    public fun onIntent(intent: JobApplicationDetailUiIntent): Unit {
        when (intent) {
            is JobApplicationDetailUiIntent.Load -> handleLoad(intent.id)
            is JobApplicationDetailUiIntent.UpdateStatus -> handleUpdateStatus(intent.newStatus, intent.date, intent.note)
            is JobApplicationDetailUiIntent.UpdateFitScore -> handleUpdateFitScore(intent.newScore)
            is JobApplicationDetailUiIntent.RecordEvent -> handleRecordEvent(intent.type, intent.notes)
            is JobApplicationDetailUiIntent.OpenArtifact -> handleOpenArtifact(intent.artifactId)
            is JobApplicationDetailUiIntent.ClickBack -> handleClickBack()
        }
    }

    private fun handleLoad(id: String): Unit {
        viewModelScope.launch {
            _uiState.value = JobApplicationDetailUiState.Loading
            try {
                val app = loadApplicationUseCase(LoadJobApplicationUseCase.Params(id = id))
                if (app == null) {
                    _uiState.value = JobApplicationDetailUiState.Error("Application not found")
                    return@launch
                }
                val companies = loadCompaniesUseCase(LoadCompaniesUseCase.Params(forceRefresh = false))
                val company = companies.find { it.name.equals(app.company, ignoreCase = true) }
                currentApplication = app
                currentCompany = company
                currentEvents = emptyList()
                currentArtifacts = emptyList()

                val model = createJobApplicationDetailUiModel(
                    app = app,
                    company = company,
                    events = currentEvents,
                    artifacts = currentArtifacts,
                )
                currentEvents = model.timelineEvents
                _uiState.value = JobApplicationDetailUiState.Content(
                    model = model,
                    isUpdating = false,
                )
            } catch (e: Exception) {
                _uiState.value = JobApplicationDetailUiState.Error(
                    message = e.message ?: "Failed to load application"
                )
            }
        }
    }

    private fun handleUpdateStatus(newStatus: TrackerStatus, date: String?, note: String?): Unit {
        val app = currentApplication ?: return
        val currentContent = _uiState.value as? JobApplicationDetailUiState.Content ?: return
        _uiState.value = currentContent.copy(isUpdating = true)
        viewModelScope.launch {
            try {
                val updated = updateStatusUseCase(
                    UpdateApplicationStatusUseCase.Params(
                        id = app.id,
                        status = newStatus,
                        date = date,
                        note = note,
                    )
                )
                currentApplication = updated
                val updatedModel = createJobApplicationDetailUiModel(
                    app = updated,
                    company = currentCompany,
                    events = currentEvents,
                    artifacts = currentArtifacts,
                )
                _uiState.value = JobApplicationDetailUiState.Content(
                    model = updatedModel,
                    isUpdating = false,
                )
            } catch (e: Exception) {
                _uiState.value = JobApplicationDetailUiState.Error(
                    message = e.message ?: "Failed to update status"
                )
            }
        }
    }

    private fun handleUpdateFitScore(newScore: Int): Unit {
        val app = currentApplication ?: return
        val currentContent = _uiState.value as? JobApplicationDetailUiState.Content ?: return
        _uiState.value = currentContent.copy(isUpdating = true)
        viewModelScope.launch {
            try {
                val updated = updateFitScoreUseCase(
                    UpdateFitScoreUseCase.Params(
                        id = app.id,
                        score = newScore,
                    )
                )
                currentApplication = updated
                val updatedModel = createJobApplicationDetailUiModel(
                    app = updated,
                    company = currentCompany,
                    events = currentEvents,
                    artifacts = currentArtifacts,
                )
                _uiState.value = JobApplicationDetailUiState.Content(
                    model = updatedModel,
                    isUpdating = false,
                )
            } catch (e: Exception) {
                _uiState.value = JobApplicationDetailUiState.Error(
                    message = e.message ?: "Failed to update fit score"
                )
            }
        }
    }

    private fun handleRecordEvent(type: TrackerEventType, notes: String): Unit {
        val app = currentApplication ?: return
        val currentContent = _uiState.value as? JobApplicationDetailUiState.Content ?: return
        _uiState.value = currentContent.copy(isUpdating = true)
        val event = Factory.createTrackerEvent(
            id = Uuid.random().toString(),
            applicationId = app.id,
            type = type,
            statusFrom = app.status,
            statusTo = null,
            note = notes.ifBlank { null },
            emailMessageId = null,
            actor = EventActor.USER,
            derivedFrom = DerivedFrom.MANUAL,
            confidence = Confidence.HIGH,
            createdAt = Clock.System.now(),
        )
        viewModelScope.launch {
            try {
                recordEventUseCase(RecordEventUseCase.Params(event = event))
                currentEvents = currentEvents + event
                val updatedModel = createJobApplicationDetailUiModel(
                    app = app,
                    company = currentCompany,
                    events = currentEvents,
                    artifacts = currentArtifacts,
                )
                _uiState.value = JobApplicationDetailUiState.Content(
                    model = updatedModel,
                    isUpdating = false,
                )
            } catch (e: Exception) {
                _uiState.value = JobApplicationDetailUiState.Error(
                    message = e.message ?: "Failed to record event"
                )
            }
        }
    }

    private fun handleOpenArtifact(artifactId: String): Unit {
        val currentContent = _uiState.value as? JobApplicationDetailUiState.Content ?: return
        val artifact = currentContent.model.artifacts.find { it.id == artifactId } ?: return
        val storagePath = artifact.storage?.path
        if (storagePath != null) {
            viewModelScope.launch {
                try {
                    getSignedUrlUseCase(GetArtifactSignedUrlUseCase.Params(path = storagePath))
                } catch (_: Exception) {
                    // Handled gracefully
                }
            }
        }
    }

    private fun handleClickBack(): Unit {
        _uiState.value = JobApplicationDetailUiState.NavigateBack
    }
}
