package dev.fnc314.tools.jobtracker.features.addjobapplication.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase

internal class CreateJobApplicationUseCaseImpl(
    private val repository: JobApplicationRepository,
) : CreateJobApplicationUseCase {
    override suspend fun invoke(params: CreateJobApplicationUseCase.Params): String =
        repository.saveJobApplication(params.application)
}
