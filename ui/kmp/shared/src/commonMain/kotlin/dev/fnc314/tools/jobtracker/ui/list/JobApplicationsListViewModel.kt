package dev.fnc314.tools.jobtracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.domain.usecase.LoadAllJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.domain.usecase.SortJobApplicationsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class JobApplicationsListViewModel(
    private val loadAll: LoadAllJobApplicationsUseCase,
    private val filterApplications: FilterJobApplicationsUseCase,
    private val sortApplications: SortJobApplicationsUseCase,
) : ViewModel(), KoinComponent {

    private val _activeFilter = MutableStateFlow(FilterJobApplicationsUseCase.FilterType.ALL)
    val activeFilter = _activeFilter.asStateFlow()

    private val _sortColumn = MutableStateFlow(SortJobApplicationsUseCase.SortColumn.DATE)
    val sortColumn = _sortColumn.asStateFlow()

    private val _sortAscending = MutableStateFlow(false)
    val sortAscending = _sortAscending.asStateFlow()

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    private val _filteredApplications = MutableStateFlow<List<JobApplication>>(emptyList())
    val filteredApplications = _filteredApplications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadApplications()
    }

    fun setFilter(filter: FilterJobApplicationsUseCase.FilterType) {
        _activeFilter.value = filter
        applyFilterAndSort()
    }

    fun toggleSortColumn(column: SortJobApplicationsUseCase.SortColumn) {
        if (_sortColumn.value == column) {
            _sortAscending.value = !_sortAscending.value
        } else {
            _sortColumn.value = column
            _sortAscending.value = false
        }
        applyFilterAndSort()
    }

    fun setSort(column: SortJobApplicationsUseCase.SortColumn, ascending: Boolean) {
        _sortColumn.value = column
        _sortAscending.value = ascending
        applyFilterAndSort()
    }

    fun refresh() {
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
            val filtered = filterApplications(FilterJobApplicationsUseCase.Params(
                applications = apps,
                filter = _activeFilter.value,
            ))
            val sorted = sortApplications(SortJobApplicationsUseCase.Params(
                applications = filtered,
                column = _sortColumn.value,
                ascending = _sortAscending.value,
            ))
            _filteredApplications.value = sorted
        }
    }
}
