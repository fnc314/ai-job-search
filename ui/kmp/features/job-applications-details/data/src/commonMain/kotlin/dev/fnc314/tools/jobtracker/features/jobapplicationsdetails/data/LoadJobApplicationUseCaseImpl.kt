package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.LoadJobApplicationUseCase

internal class LoadJobApplicationUseCaseImpl(
    private val repository: JobApplicationRepository,
) : LoadJobApplicationUseCase {
    override suspend fun invoke(params: LoadJobApplicationUseCase.Params): JobApplication? =
        repository.getJobApplication(params.id)
}
