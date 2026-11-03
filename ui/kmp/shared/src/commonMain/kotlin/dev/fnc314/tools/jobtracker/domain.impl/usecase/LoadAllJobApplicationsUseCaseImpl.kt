package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.domain.usecase.LoadAllJobApplicationsUseCase

internal class LoadAllJobApplicationsUseCaseImpl(
    private val repository: JobApplicationRepository,
) : LoadAllJobApplicationsUseCase {
    override suspend fun invoke(params: LoadAllJobApplicationsUseCase.Params): List<JobApplication> =
        repository.getAllJobApplications()
}
