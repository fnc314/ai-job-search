package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.impl

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.repository.JobApplicationsListRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.LoadAllJobApplicationsUseCase

public class LoadAllJobApplicationsUseCaseImpl(
    private val repository: JobApplicationsListRepository,
) : LoadAllJobApplicationsUseCase {
    override suspend fun invoke(params: LoadAllJobApplicationsUseCase.Params): List<JobApplication> =
        repository(forceRefresh = params.forceRefresh)
}
