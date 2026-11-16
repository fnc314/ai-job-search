package dev.fnc314.aijobtracker.frontend.features.companydetails.ui

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface CompanyDetailsUiState {
    data object Loading : CompanyDetailsUiState
    data class Error(val message: String) : CompanyDetailsUiState
    data class Content(
        val details: CompanyDetailsUiModel,
        val isRefreshing: Boolean = false,
    ) : CompanyDetailsUiState
}
