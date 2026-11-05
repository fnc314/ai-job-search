package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase.ApplicationAge
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase.SortColumn
import kotlinx.collections.immutable.ImmutableList

/**
 * Reactive UI state representations for the job applications dashboard screen.
 */
public sealed interface JobApplicationsListUiState {

    /** State rendered while loading applications from cache or network. */
    public data object Loading : JobApplicationsListUiState

    /**
     * Active content state rendering the list of applications with current filter and sort configurations.
     *
     * @property applications Immutable list of decoupled item models [JobApplicationItemUiModel].
     * @property statusFilter Currently selected status filter [TrackerStatus].
     * @property ageFilter Currently selected temporal age filter [ApplicationAge].
     * @property sortColumn Active sort column [SortColumn].
     * @property sortAscending Sort order flag (true for ascending, false for descending).
     * @property isRefreshing Whether a swipe-to-refresh or pull-to-refresh operation is underway.
     */
    public data class Content(
        public val applications: ImmutableList<JobApplicationItemUiModel>,
        public val statusFilter: TrackerStatus,
        public val ageFilter: ApplicationAge,
        public val sortColumn: SortColumn,
        public val sortAscending: Boolean,
        public val isRefreshing: Boolean,
    ) : JobApplicationsListUiState

    /**
     * Error state rendered when fetching applications fails.
     *
     * @property message Descriptive error message for user display.
     */
    public data class Error(public val message: String) : JobApplicationsListUiState

    /** Navigation side-effect state navigating to the detail view for [applicationId]. */
    public data class NavigateToDetail(public val applicationId: String) : JobApplicationsListUiState

    /** Navigation side-effect state navigating to the Add Application screen. */
    public data object NavigateToAddApplication : JobApplicationsListUiState

    /** Navigation side-effect state navigating to the Add Lead screen. */
    public data object NavigateToAddLead : JobApplicationsListUiState

    /** Navigation side-effect state navigating to the Pending Edits screen. */
    public data object NavigateToEdits : JobApplicationsListUiState

    /** Navigation side-effect state navigating to the Settings screen. */
    public data object NavigateToSettings : JobApplicationsListUiState
}
