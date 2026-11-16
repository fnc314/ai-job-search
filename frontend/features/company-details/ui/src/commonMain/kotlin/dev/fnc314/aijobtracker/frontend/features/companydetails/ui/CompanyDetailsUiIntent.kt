package dev.fnc314.aijobtracker.frontend.features.companydetails.ui

internal sealed interface CompanyDetailsUiIntent {
    data object Refresh : CompanyDetailsUiIntent
    data class SelectApplication(val applicationId: String) : CompanyDetailsUiIntent
}
