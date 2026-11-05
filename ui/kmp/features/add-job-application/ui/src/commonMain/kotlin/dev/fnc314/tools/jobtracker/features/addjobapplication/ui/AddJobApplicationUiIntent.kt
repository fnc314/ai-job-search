package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import kotlinx.collections.immutable.ImmutableList

public sealed interface AddJobApplicationUiIntent {
    public data class UpdateCompany(public val value: String) : AddJobApplicationUiIntent
    public data class UpdateRole(public val value: String) : AddJobApplicationUiIntent
    public data class UpdateSector(public val value: String) : AddJobApplicationUiIntent
    public data class UpdateRoleType(public val value: String) : AddJobApplicationUiIntent
    public data class UpdateWorkMode(public val mode: WorkMode) : AddJobApplicationUiIntent
    public data class UpdateChannel(public val channel: ApplicationChannel) : AddJobApplicationUiIntent
    public data class UpdateStatus(public val status: TrackerStatus) : AddJobApplicationUiIntent
    public data class UpdateJobUrl(public val value: String) : AddJobApplicationUiIntent
    public data class UpdateJobApplicationUrl(public val value: String) : AddJobApplicationUiIntent
    public data class UpdateJobId(public val value: String) : AddJobApplicationUiIntent
    public data class UpdateLocation(public val value: String) : AddJobApplicationUiIntent
    public data class UpdateNotes(public val notes: ImmutableList<String>) : AddJobApplicationUiIntent
    public data class UpdateTags(public val value: String) : AddJobApplicationUiIntent
    public data object Submit : AddJobApplicationUiIntent
    public data object ClickBack : AddJobApplicationUiIntent
    public data object DismissError : AddJobApplicationUiIntent
}
