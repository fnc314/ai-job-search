package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase

class SortJobApplicationsUseCaseImpl : SortJobApplicationsUseCase {
    override suspend fun invoke(params: SortJobApplicationsUseCase.Params): List<JobApplication> =
        params.sort()
}
