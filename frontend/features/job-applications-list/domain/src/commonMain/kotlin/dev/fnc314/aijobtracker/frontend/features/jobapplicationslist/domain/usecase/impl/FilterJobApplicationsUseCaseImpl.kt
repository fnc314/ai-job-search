package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase

internal class FilterJobApplicationsUseCaseImpl : FilterJobApplicationsUseCase {
    override suspend fun invoke(params: FilterJobApplicationsUseCase.Params): List<JobApplication> =
        params.filter()
}
