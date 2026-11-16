package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.data.repository.JobApplicationsListRepository
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.domain.usecase.LoadAllJobApplicationsUseCase

internal class LoadAllJobApplicationsUseCaseImpl(
    private val repository: JobApplicationsListRepository,
) : LoadAllJobApplicationsUseCase {
    override suspend fun invoke(params: LoadAllJobApplicationsUseCase.Params): List<JobApplication> =
        repository(forceRefresh = params.forceRefresh)
}
