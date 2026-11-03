package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.domain.impl.filter.FilterUtil
import dev.fnc314.tools.jobtracker.domain.usecase.FilterJobApplicationsUseCase

internal class FilterJobApplicationsUseCaseImpl : FilterJobApplicationsUseCase {
    override suspend fun invoke(params: FilterJobApplicationsUseCase.Params): List<JobApplication> =
        when (params.filter) {
            FilterJobApplicationsUseCase.FilterType.ALL -> params.applications
            FilterJobApplicationsUseCase.FilterType.OPEN -> FilterUtil.filterOpen(params.applications)
            FilterJobApplicationsUseCase.FilterType.LEADS -> FilterUtil.filterLeads(params.applications)
            FilterJobApplicationsUseCase.FilterType.REJECTED -> FilterUtil.filterRejected(params.applications)
        }
}
