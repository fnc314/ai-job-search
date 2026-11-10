package dev.fnc314.tools.jobtracker.features.companieslist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.features.companieslist.domain.CompanySortOption
import dev.fnc314.tools.jobtracker.features.companieslist.domain.FilterCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.companieslist.domain.LoadAllCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.companieslist.domain.SortCompaniesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public class CompaniesListViewModel(
    private val loadAllCompanies: LoadAllCompaniesUseCase,
    private val filterCompanies: FilterCompaniesUseCase,
    private val sortCompanies: SortCompaniesUseCase,
) : ViewModel() {

    private val _uiState: MutableStateFlow<CompaniesListUiState> =
        MutableStateFlow(CompaniesListUiState.Loading)
    public val uiState: StateFlow<CompaniesListUiState> = _uiState.asStateFlow()

    private var rawCompanies: List<Company> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentSortOption: CompanySortOption = CompanySortOption.NAME_ASC
    private val expandedCompanyIds = mutableSetOf<String>()

    init {
        loadCompanies(forceRefresh = false)
    }

    public fun onIntent(intent: CompaniesListUiIntent) {
        when (intent) {
            is CompaniesListUiIntent.Refresh -> {
                loadCompanies(forceRefresh = true)
            }
            is CompaniesListUiIntent.Search -> {
                currentSearchQuery = intent.query
                recalculateContent(isRefreshing = false)
            }
            is CompaniesListUiIntent.ChangeSort -> {
                currentSortOption = intent.sortOption
                recalculateContent(isRefreshing = false)
            }
            is CompaniesListUiIntent.ToggleExpand -> {
                if (expandedCompanyIds.contains(intent.companyId)) {
                    expandedCompanyIds.remove(intent.companyId)
                } else {
                    expandedCompanyIds.add(intent.companyId)
                }
                recalculateContent(isRefreshing = false)
            }
            is CompaniesListUiIntent.SelectCompany -> {
                // Handled in screen callback directly
            }
        }
    }

    private fun loadCompanies(forceRefresh: Boolean) {
        viewModelScope.launch {
            if (forceRefresh) {
                val current = _uiState.value
                if (current is CompaniesListUiState.Content) {
                    _uiState.value = current.copy(isRefreshing = true)
                }
            } else {
                _uiState.value = CompaniesListUiState.Loading
            }

            runCatching {
                loadAllCompanies(forceRefresh = forceRefresh)
            }.onSuccess { companies ->
                rawCompanies = companies
                recalculateContent(isRefreshing = false)
            }.onFailure { error ->
                _uiState.value = CompaniesListUiState.Error(
                    message = error.message ?: "Failed to load companies"
                )
            }
        }
    }

    private fun recalculateContent(isRefreshing: Boolean) {
        val filtered = filterCompanies(rawCompanies, currentSearchQuery)
        val sorted = sortCompanies(filtered, currentSortOption)
        val uiModels = sorted.map { company ->
            company.toUiModel(isExpanded = expandedCompanyIds.contains(company.id))
        }

        _uiState.value = CompaniesListUiState.Content(
            companies = uiModels,
            isRefreshing = isRefreshing,
            searchQuery = currentSearchQuery,
            sortOption = currentSortOption,
        )
    }
}
