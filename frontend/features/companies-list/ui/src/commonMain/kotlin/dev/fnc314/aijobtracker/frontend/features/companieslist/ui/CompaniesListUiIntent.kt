package dev.fnc314.aijobtracker.frontend.features.companieslist.ui

import dev.fnc314.aijobtracker.frontend.features.companieslist.domain.CompanySortOption
import dev.fnc314.aijobtracker.frontend.features.companieslist.domain.CompanyStatusFilter

internal sealed interface CompaniesListUiIntent {
    data object Refresh : CompaniesListUiIntent
    data class Search(val query: String) : CompaniesListUiIntent
    data class ChangeSort(val sortOption: CompanySortOption) : CompaniesListUiIntent
    data class ChangeStatusFilter(val filter: CompanyStatusFilter) : CompaniesListUiIntent
    data class ToggleExpand(val companyId: String) : CompaniesListUiIntent
    data class SelectCompany(
        val companyId: String,
        val companyName: String,
        val website: String?,
    ) : CompaniesListUiIntent
}
