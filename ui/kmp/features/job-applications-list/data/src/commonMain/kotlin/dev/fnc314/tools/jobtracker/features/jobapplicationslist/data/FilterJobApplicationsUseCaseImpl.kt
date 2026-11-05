package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase

class FilterJobApplicationsUseCaseImpl : FilterJobApplicationsUseCase {
    override suspend fun invoke(params: FilterJobApplicationsUseCase.Params): List<JobApplication> =
        params.filter()
}
