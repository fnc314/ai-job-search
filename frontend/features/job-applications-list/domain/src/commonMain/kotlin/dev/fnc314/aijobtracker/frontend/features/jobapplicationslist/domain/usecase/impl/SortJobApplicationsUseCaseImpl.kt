package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase

internal class SortJobApplicationsUseCaseImpl : SortJobApplicationsUseCase {
    override suspend fun invoke(params: SortJobApplicationsUseCase.Params): List<JobApplication> =
        params.sort()
}
