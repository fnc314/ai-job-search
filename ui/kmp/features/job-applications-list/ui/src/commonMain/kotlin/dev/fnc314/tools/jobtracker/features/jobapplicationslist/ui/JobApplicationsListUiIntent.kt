package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase.ApplicationAge
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase.SortColumn

public sealed interface JobApplicationsListUiIntent {
    public data class SelectApplication(public val id: String) : JobApplicationsListUiIntent

    public data class ChangeFilter(
        public val status: TrackerStatus,
        public val age: ApplicationAge,
    ) : JobApplicationsListUiIntent

    public data class ChangeSort(
        public val column: SortColumn,
        public val ascending: Boolean,
    ) : JobApplicationsListUiIntent

    public data object Refresh : JobApplicationsListUiIntent

    public data object ClickAddApplication : JobApplicationsListUiIntent

    public data object ClickAddLead : JobApplicationsListUiIntent

    public data object ClickEdits : JobApplicationsListUiIntent

    public data object ClickSettings : JobApplicationsListUiIntent
}
