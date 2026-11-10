package dev.fnc314.tools.jobtracker.features.companieslist.ui

import dev.fnc314.tools.jobtracker.features.companieslist.domain.CompanySortOption
import dev.fnc314.tools.jobtracker.features.companieslist.domain.CompanyStatusFilter

public sealed interface CompaniesListUiIntent {
    public data object Refresh : CompaniesListUiIntent
    public data class Search(public val query: String) : CompaniesListUiIntent
    public data class ChangeSort(public val sortOption: CompanySortOption) : CompaniesListUiIntent
    public data class ChangeStatusFilter(public val filter: CompanyStatusFilter) : CompaniesListUiIntent
    public data class ToggleExpand(public val companyId: String) : CompaniesListUiIntent
    public data class SelectCompany(
        public val companyId: String,
        public val companyName: String,
        public val website: String?,
    ) : CompaniesListUiIntent
}
