package dev.fnc314.tools.jobtracker.features.companydetails.ui

public sealed interface CompanyDetailsUiIntent {
    public data object Refresh : CompanyDetailsUiIntent
    public data class SelectApplication(public val applicationId: String) : CompanyDetailsUiIntent
}
