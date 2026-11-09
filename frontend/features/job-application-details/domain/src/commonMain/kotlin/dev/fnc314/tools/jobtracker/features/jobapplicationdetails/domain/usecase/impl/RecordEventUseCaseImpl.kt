package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.RecordEventUseCase

internal class RecordEventUseCaseImpl(
  private val repository: JobApplicationDetailsRepository,
) : RecordEventUseCase {
    override suspend fun invoke(params: RecordEventUseCase.Params): String =
        repository.recordEvent(params.event)
}
