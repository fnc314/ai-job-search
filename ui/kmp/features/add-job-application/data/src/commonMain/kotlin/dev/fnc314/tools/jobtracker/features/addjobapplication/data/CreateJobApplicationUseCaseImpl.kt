package dev.fnc314.tools.jobtracker.features.addjobapplication.data

import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.repository.AddJobApplicationRepository
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase

public class CreateJobApplicationUseCaseImpl(
    private val repository: AddJobApplicationRepository,
) : CreateJobApplicationUseCase {
    override suspend fun invoke(params: CreateJobApplicationUseCase.Params): String =
        repository.saveJobApplication(params.application)
}
