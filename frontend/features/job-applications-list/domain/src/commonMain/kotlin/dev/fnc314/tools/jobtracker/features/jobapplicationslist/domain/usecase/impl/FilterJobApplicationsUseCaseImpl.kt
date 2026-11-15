package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.impl

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase

internal class FilterJobApplicationsUseCaseImpl : FilterJobApplicationsUseCase {
    override suspend fun invoke(params: FilterJobApplicationsUseCase.Params): List<JobApplication> =
        params.filter()
}
