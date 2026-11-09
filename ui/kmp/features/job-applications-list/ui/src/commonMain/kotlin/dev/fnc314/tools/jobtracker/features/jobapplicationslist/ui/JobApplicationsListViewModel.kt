package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.displayName
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase.ApplicationAge
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.LoadAllJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase.SortColumn
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Architecture ViewModel coordinating state and intents for the Job Applications List dashboard.
 *
 * Implements MVI unidirectional data flow: processes [JobApplicationsListUiIntent] actions,
 * executes domain use cases ([LoadAllJobApplicationsUseCase], [FilterJobApplicationsUseCase], [SortJobApplicationsUseCase]),
 * and exposes reactive UI state via [uiState].
 */
public class JobApplicationsListViewModel(
    private val loadAll: LoadAllJobApplicationsUseCase,
    private val filterApplications: FilterJobApplicationsUseCase,
    private val sortApplications: SortJobApplicationsUseCase,
) : ViewModel() {

    private val _uiState: MutableStateFlow<JobApplicationsListUiState> =
        MutableStateFlow(JobApplicationsListUiState.Loading)

    /**
     * Observable stream emitting current [JobApplicationsListUiState] updates.
     */
    public val uiState: StateFlow<JobApplicationsListUiState> = _uiState.asStateFlow()

    private var rawApplications: List<JobApplication> = emptyList()
    private var currentStatusFilter: TrackerStatus = TrackerStatus.NONE
    private var currentAgeFilter: ApplicationAge = ApplicationAge.NONE
    private var currentSortColumn: SortColumn = SortColumn.DATE_CREATED
    private var currentSortAscending: Boolean = false

    init {
        loadApplications(isRefresh = false)
    }

    /**
     * Processes user interaction intents dispatched from the UI.
     *
     * @param intent The [JobApplicationsListUiIntent] to handle.
     */
    public fun onIntent(intent: JobApplicationsListUiIntent): Unit {
        when (intent) {
            is JobApplicationsListUiIntent.SelectApplication -> {
                _uiState.value = JobApplicationsListUiState.NavigateToDetail(intent.id)
            }
            is JobApplicationsListUiIntent.ChangeFilter -> {
                currentStatusFilter = intent.status
                currentAgeFilter = intent.age
                applyFilterAndSort(isRefreshing = false)
            }
            is JobApplicationsListUiIntent.ChangeSort -> {
                currentSortColumn = intent.column
                currentSortAscending = intent.ascending
                applyFilterAndSort(isRefreshing = false)
            }
            JobApplicationsListUiIntent.Refresh -> {
                currentSortColumn = SortColumn.DATE_CREATED
                currentSortAscending = false
                loadApplications(isRefresh = true)
            }
            JobApplicationsListUiIntent.ClickAddApplication -> {
                _uiState.value = JobApplicationsListUiState.NavigateToAddApplication
            }
            JobApplicationsListUiIntent.ClickAddLead -> {
                _uiState.value = JobApplicationsListUiState.NavigateToAddLead
            }
            JobApplicationsListUiIntent.ClickEdits -> {
                _uiState.value = JobApplicationsListUiState.NavigateToEdits
            }
            JobApplicationsListUiIntent.ClickSettings -> {
                _uiState.value = JobApplicationsListUiState.NavigateToSettings
            }
        }
    }

    private fun loadApplications(isRefresh: Boolean): Unit {
        viewModelScope.launch {
            if (!isRefresh) {
                _uiState.value = JobApplicationsListUiState.Loading
            } else {
                val current = _uiState.value
                if (current is JobApplicationsListUiState.Content) {
                    _uiState.value = current.copy(isRefreshing = true)
                }
            }
            try {
                val apps = loadAll(LoadAllJobApplicationsUseCase.Params(forceRefresh = true))
                rawApplications = apps
                applyFilterAndSort(isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = JobApplicationsListUiState.Error(
                    message = e.message ?: "Failed to load applications",
                )
            }
        }
    }

    private fun applyFilterAndSort(isRefreshing: Boolean): Unit {
        viewModelScope.launch {
            try {
                val filtered = filterApplications(
                    FilterJobApplicationsUseCase.Params(
                        applications = rawApplications,
                        status = currentStatusFilter,
                        age = currentAgeFilter,
                    ),
                )
                val sorted = sortApplications(
                    SortJobApplicationsUseCase.Params(
                        applications = filtered,
                        column = currentSortColumn,
                        ascending = currentSortAscending,
                    ),
                )
                val uiModels = sorted.map { it.toUiModel() }.toImmutableList()
                _uiState.value = JobApplicationsListUiState.Content(
                    applications = uiModels,
                    statusFilter = currentStatusFilter,
                    ageFilter = currentAgeFilter,
                    sortColumn = currentSortColumn,
                    sortAscending = currentSortAscending,
                    isRefreshing = isRefreshing,
                )
            } catch (e: Exception) {
                _uiState.value = JobApplicationsListUiState.Error(
                    message = e.message ?: "Failed to filter applications",
                )
            }
        }
    }

    private fun JobApplication.toUiModel(): JobApplicationItemUiModel {
        return JobApplicationItemUiModel(
            id = id,
            companyName = company,
            roleTitle = role ?: "",
            locationText = location,
            status = status,
            statusDisplayName = status.displayName(),
            fitScore = fitScore,
            fitScoreText = fitScore?.let { "Fit $it" },
            createdAt = createdAt,
            updatedAt = updatedAt,
            firstActivityAt = timeline?.firstActivityAt ?: Instant.DISTANT_PAST,
            onClickIntent = JobApplicationsListUiIntent.SelectApplication(id),
        )
    }
}
