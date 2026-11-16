package dev.fnc314.aijobtracker.frontend.features.companieslist.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.aijobtracker.frontend.features.companieslist.domain.CompanySortOption
import dev.fnc314.aijobtracker.frontend.features.companieslist.domain.CompanyStatusFilter

@Immutable
internal sealed interface CompaniesListUiState {
    data object Loading : CompaniesListUiState

    data class Error(val message: String) : CompaniesListUiState

    data class Content(
        val companies: List<CompanyCardUiModel>,
        val isRefreshing: Boolean = false,
        val searchQuery: String = "",
        val sortOption: CompanySortOption = CompanySortOption.NAME_ASC,
        val statusFilter: CompanyStatusFilter = CompanyStatusFilter.ALL,
    ) : CompaniesListUiState
}
