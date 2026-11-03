package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.domain.usecase.LoadJobApplicationUseCase

internal class LoadJobApplicationUseCaseImpl(
    private val repository: JobApplicationRepository,
) : LoadJobApplicationUseCase {
    override suspend fun invoke(params: LoadJobApplicationUseCase.Params): JobApplication? =
        repository.getJobApplication(params.id)
}
