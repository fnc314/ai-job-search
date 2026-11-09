package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import kotlinx.collections.immutable.ImmutableList

/**
 * User-initiated intents dispatched to [AddLeadViewModel].
 */
public sealed interface AddLeadUiIntent {
    /** Updates the company name field. */
    public data class UpdateCompany(public val value: String) : AddLeadUiIntent
    /** Updates primary recruiter or interviewer contact details. */
    public data class UpdateContact(public val value: String) : AddLeadUiIntent
    /** Updates the target job title or prospective role. */
    public data class UpdateRole(public val value: String) : AddLeadUiIntent
    /** Updates the communication or lead acquisition channel. */
    public data class UpdateChannel(public val value: String) : AddLeadUiIntent
    /** Updates the attached freeform notes list. */
    public data class UpdateNotes(public val notes: ImmutableList<String>) : AddLeadUiIntent
    /** Updates comma-separated search tags. */
    public data class UpdateTags(public val value: String) : AddLeadUiIntent
    /** Triggers validation and asynchronous submission of the lead. */
    public data object Submit : AddLeadUiIntent
    /** Requests back navigation without submitting. */
    public data object ClickBack : AddLeadUiIntent
    /** Clears the current validation or submission error message. */
    public data object DismissError : AddLeadUiIntent
}
