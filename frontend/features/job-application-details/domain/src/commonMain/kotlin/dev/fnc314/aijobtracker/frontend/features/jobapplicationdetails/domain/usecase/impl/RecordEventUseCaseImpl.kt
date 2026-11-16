package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.RecordEventUseCase

internal class RecordEventUseCaseImpl(
  private val repository: JobApplicationDetailsRepository,
) : RecordEventUseCase {
    override suspend fun invoke(params: RecordEventUseCase.Params): String =
        repository.recordEvent(params.event)
}
