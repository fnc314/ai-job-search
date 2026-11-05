package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import kotlinx.collections.immutable.ImmutableList

public sealed interface AddLeadUiIntent {
    public data class UpdateCompany(public val value: String) : AddLeadUiIntent
    public data class UpdateContact(public val value: String) : AddLeadUiIntent
    public data class UpdateRole(public val value: String) : AddLeadUiIntent
    public data class UpdateChannel(public val value: String) : AddLeadUiIntent
    public data class UpdateNotes(public val notes: ImmutableList<String>) : AddLeadUiIntent
    public data class UpdateTags(public val value: String) : AddLeadUiIntent
    public data object Submit : AddLeadUiIntent
    public data object ClickBack : AddLeadUiIntent
    public data object DismissError : AddLeadUiIntent
}
