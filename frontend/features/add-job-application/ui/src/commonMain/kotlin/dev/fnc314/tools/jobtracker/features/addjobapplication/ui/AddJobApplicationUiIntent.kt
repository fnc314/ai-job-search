package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import kotlinx.collections.immutable.ImmutableList

/**
 * User-initiated intents dispatched to [AddJobApplicationViewModel].
 */
public sealed interface AddJobApplicationUiIntent {
    /** Updates the company name field. */
    public data class UpdateCompany(public val value: String) : AddJobApplicationUiIntent
    /** Updates the target job title or role. */
    public data class UpdateRole(public val value: String) : AddJobApplicationUiIntent
    /** Updates the industry sector categorization. */
    public data class UpdateSector(public val value: String) : AddJobApplicationUiIntent
    /** Updates the role classification (e.g. Lead, Staff, Architect). */
    public data class UpdateRoleType(public val value: String) : AddJobApplicationUiIntent
    /** Updates the work mode arrangement (Remote, Hybrid, Onsite). */
    public data class UpdateWorkMode(public val mode: WorkMode) : AddJobApplicationUiIntent
    /** Updates the application submission channel. */
    public data class UpdateChannel(public val channel: ApplicationChannel) : AddJobApplicationUiIntent
    /** Updates the initial tracker lifecycle status. */
    public data class UpdateStatus(public val status: TrackerStatus) : AddJobApplicationUiIntent
    /** Updates the source job listing URL. */
    public data class UpdateJobUrl(public val value: String) : AddJobApplicationUiIntent
    /** Updates the direct application submission URL. */
    public data class UpdateJobApplicationUrl(public val value: String) : AddJobApplicationUiIntent
    /** Updates the external applicant tracking system requisition ID. */
    public data class UpdateJobId(public val value: String) : AddJobApplicationUiIntent
    /** Updates the primary geographic work location or restriction. */
    public data class UpdateLocation(public val value: String) : AddJobApplicationUiIntent
    /** Updates the attached freeform notes list. */
    public data class UpdateNotes(public val notes: ImmutableList<String>) : AddJobApplicationUiIntent
    /** Updates comma-separated search tags. */
    public data class UpdateTags(public val value: String) : AddJobApplicationUiIntent
    /** Triggers validation and asynchronous submission of the form. */
    public data object Submit : AddJobApplicationUiIntent
    /** Requests back navigation without submitting. */
    public data object ClickBack : AddJobApplicationUiIntent
    /** Clears the current validation or submission error message. */
    public data object DismissError : AddJobApplicationUiIntent
}
