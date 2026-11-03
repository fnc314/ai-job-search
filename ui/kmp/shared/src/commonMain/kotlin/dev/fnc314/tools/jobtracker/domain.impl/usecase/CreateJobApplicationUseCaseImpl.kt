package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.domain.usecase.CreateJobApplicationUseCase

internal class CreateJobApplicationUseCaseImpl(
    private val repository: JobApplicationRepository,
) : CreateJobApplicationUseCase {
    override suspend fun invoke(params: CreateJobApplicationUseCase.Params): String =
        repository.saveJobApplication(params.application)
}
