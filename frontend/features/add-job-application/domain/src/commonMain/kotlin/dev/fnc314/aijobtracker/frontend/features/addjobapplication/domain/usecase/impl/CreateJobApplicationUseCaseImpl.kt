package dev.fnc314.aijobtracker.frontend.features.addjobapplication.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.features.addjobapplication.domain.repository.AddJobApplicationRepository
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase

internal class CreateJobApplicationUseCaseImpl(
    private val repository: AddJobApplicationRepository,
) : CreateJobApplicationUseCase {
    override suspend fun invoke(params: CreateJobApplicationUseCase.Params): String =
        repository.saveJobApplication(params.application)
}
