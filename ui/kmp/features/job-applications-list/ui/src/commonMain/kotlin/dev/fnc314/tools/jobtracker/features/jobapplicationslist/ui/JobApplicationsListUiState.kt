package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase.ApplicationAge
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase.SortColumn
import kotlinx.collections.immutable.ImmutableList

public sealed interface JobApplicationsListUiState {
    public data object Loading : JobApplicationsListUiState

    public data class Content(
        public val applications: ImmutableList<JobApplicationItemUiModel>,
        public val statusFilter: TrackerStatus,
        public val ageFilter: ApplicationAge,
        public val sortColumn: SortColumn,
        public val sortAscending: Boolean,
        public val isRefreshing: Boolean,
    ) : JobApplicationsListUiState

    public data class Error(public val message: String) : JobApplicationsListUiState

    public data class NavigateToDetail(public val applicationId: String) : JobApplicationsListUiState

    public data object NavigateToAddApplication : JobApplicationsListUiState

    public data object NavigateToAddLead : JobApplicationsListUiState

    public data object NavigateToEdits : JobApplicationsListUiState

    public data object NavigateToSettings : JobApplicationsListUiState
}
