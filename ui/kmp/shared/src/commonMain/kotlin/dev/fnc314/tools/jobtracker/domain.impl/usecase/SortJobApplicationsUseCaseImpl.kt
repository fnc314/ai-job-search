package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.domain.impl.filter.SortUtil
import dev.fnc314.tools.jobtracker.domain.usecase.SortJobApplicationsUseCase

internal class SortJobApplicationsUseCaseImpl : SortJobApplicationsUseCase {
    override suspend fun invoke(params: SortJobApplicationsUseCase.Params): List<JobApplication> =
        when (params.column) {
            SortJobApplicationsUseCase.SortColumn.DATE -> SortUtil.sortByDate(params.applications, params.ascending)
            SortJobApplicationsUseCase.SortColumn.COMPANY -> SortUtil.sortByCompany(params.applications, params.ascending)
            SortJobApplicationsUseCase.SortColumn.STATUS -> SortUtil.sortByStatus(params.applications, params.ascending)
            SortJobApplicationsUseCase.SortColumn.FIT -> SortUtil.sortByFit(params.applications, params.ascending)
            SortJobApplicationsUseCase.SortColumn.ROLE -> SortUtil.sortByRole(params.applications, params.ascending)
        }
}
