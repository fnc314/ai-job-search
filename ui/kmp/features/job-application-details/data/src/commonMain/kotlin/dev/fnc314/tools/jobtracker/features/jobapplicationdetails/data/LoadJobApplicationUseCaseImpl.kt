package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadJobApplicationUseCase

public class LoadJobApplicationUseCaseImpl(
    private val repository: JobApplicationsDetailsRepository,
) : LoadJobApplicationUseCase {
    override suspend fun invoke(params: LoadJobApplicationUseCase.Params): JobApplication? =
        repository.getJobApplication(params.id)
}
