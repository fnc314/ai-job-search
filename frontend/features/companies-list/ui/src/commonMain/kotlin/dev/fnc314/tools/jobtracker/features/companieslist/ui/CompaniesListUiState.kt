package dev.fnc314.tools.jobtracker.features.companieslist.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.features.companieslist.domain.CompanySortOption
import dev.fnc314.tools.jobtracker.features.companieslist.domain.CompanyStatusFilter

@Immutable
public sealed interface CompaniesListUiState {
    public data object Loading : CompaniesListUiState

    public data class Error(public val message: String) : CompaniesListUiState

    public data class Content(
        public val companies: List<CompanyCardUiModel>,
        public val isRefreshing: Boolean = false,
        public val searchQuery: String = "",
        public val sortOption: CompanySortOption = CompanySortOption.NAME_ASC,
        public val statusFilter: CompanyStatusFilter = CompanyStatusFilter.ALL,
    ) : CompaniesListUiState
}
