package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.LoadAllJobApplicationsUseCase

class LoadAllJobApplicationsUseCaseImpl(
    private val repository: JobApplicationRepository,
) : LoadAllJobApplicationsUseCase {
    override suspend fun invoke(params: LoadAllJobApplicationsUseCase.Params): List<JobApplication> =
        repository.getAllJobApplications()
}
