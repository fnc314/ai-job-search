package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.UpdateApplicationStatusUseCase

internal class UpdateApplicationStatusUseCaseImpl(
  private val repository: JobApplicationDetailsRepository,
) : UpdateApplicationStatusUseCase {
    override suspend fun invoke(params: UpdateApplicationStatusUseCase.Params): JobApplication =
        repository.updateStatus(params.id, params.status, params.date, params.note)
}
