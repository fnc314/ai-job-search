package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase.ApplicationAge
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase.SortColumn

/**
 * User interactions and events emitted from the Job Applications List UI screen.
 */
public sealed interface JobApplicationsListUiIntent {

    /**
     * User selected an individual application card to inspect its details.
     *
     * @property id Application slug or UUID identifier.
     */
    public data class SelectApplication(public val id: String) : JobApplicationsListUiIntent

    /**
     * User modified filter criteria.
     *
     * @property status Desired status filter [TrackerStatus].
     * @property age Desired age filter [ApplicationAge].
     */
    public data class ChangeFilter(
        public val status: TrackerStatus,
        public val age: ApplicationAge,
    ) : JobApplicationsListUiIntent

    /**
     * User changed column sorting or toggled sort direction.
     *
     * @property column Target sort column [SortColumn].
     * @property ascending Sort direction flag.
     */
    public data class ChangeSort(
        public val column: SortColumn,
        public val ascending: Boolean,
    ) : JobApplicationsListUiIntent

    /** User triggered manual list refresh. */
    public data object Refresh : JobApplicationsListUiIntent

    /** User clicked button to add a new formal application. */
    public data object ClickAddApplication : JobApplicationsListUiIntent

    /** User clicked button to add an informal lead. */
    public data object ClickAddLead : JobApplicationsListUiIntent

    /** User clicked button to inspect pending edits. */
    public data object ClickEdits : JobApplicationsListUiIntent

    /** User clicked button to open application preferences. */
    public data object ClickSettings : JobApplicationsListUiIntent
}
