package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.LoadAllJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public class JobApplicationsListViewModel(
    private val loadAll: LoadAllJobApplicationsUseCase,
    private val filterApplications: FilterJobApplicationsUseCase,
    private val sortApplications: SortJobApplicationsUseCase,
) : ViewModel() {

    private val _statusFilter = MutableStateFlow(TrackerStatus.NONE)
    public val statusFilter: StateFlow<TrackerStatus> = _statusFilter.asStateFlow()

    private val _ageFilter = MutableStateFlow(FilterJobApplicationsUseCase.ApplicationAge.NONE)
    public val ageFilter: StateFlow<FilterJobApplicationsUseCase.ApplicationAge> = _ageFilter.asStateFlow()

    private val _sortColumn = MutableStateFlow(SortJobApplicationsUseCase.SortColumn.DATE_CREATED)
    public val sortColumn: StateFlow<SortJobApplicationsUseCase.SortColumn> = _sortColumn.asStateFlow()

    private val _sortAscending = MutableStateFlow(false)
    public val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    private val _filteredApplications = MutableStateFlow<List<JobApplication>>(emptyList())
    public val filteredApplications: StateFlow<List<JobApplication>> = _filteredApplications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    public val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    public val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadApplications()
    }

    public fun setFilter(
        status: TrackerStatus = _statusFilter.value,
        age: FilterJobApplicationsUseCase.ApplicationAge = _ageFilter.value,
    ) {
        _statusFilter.value = status
        _ageFilter.value = age
        applyFilterAndSort()
    }

    public fun setSort(column: SortJobApplicationsUseCase.SortColumn, ascending: Boolean) {
        _sortColumn.value = column
        _sortAscending.value = ascending
        applyFilterAndSort()
    }

    public fun refresh() {
        // Reset to initial sort order: newest -> oldest createdAt
        _sortColumn.value = SortJobApplicationsUseCase.SortColumn.DATE_CREATED
        _sortAscending.value = false
        loadApplications()
    }

    private fun loadApplications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val apps = loadAll(LoadAllJobApplicationsUseCase.Params(forceRefresh = true))
                _applications.value = apps
                applyFilterAndSort()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load applications"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyFilterAndSort() {
        viewModelScope.launch {
            val apps = _applications.value
            val filtered = filterApplications(
                FilterJobApplicationsUseCase.Params(
                    applications = apps,
                    status = _statusFilter.value,
                    age = _ageFilter.value,
                )
            )
            val sorted = sortApplications(
                SortJobApplicationsUseCase.Params(
                    applications = filtered,
                    column = _sortColumn.value,
                    ascending = _sortAscending.value,
                )
            )
            _filteredApplications.value = sorted
        }
    }
}
