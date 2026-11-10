package dev.fnc314.tools.jobtracker.features.companydetails.ui

import androidx.compose.runtime.Immutable

@Immutable
public sealed interface CompanyDetailsUiState {
    public data object Loading : CompanyDetailsUiState
    public data class Error(public val message: String) : CompanyDetailsUiState
    public data class Content(
        public val details: CompanyDetailsUiModel,
        public val isRefreshing: Boolean = false,
    ) : CompanyDetailsUiState
}
