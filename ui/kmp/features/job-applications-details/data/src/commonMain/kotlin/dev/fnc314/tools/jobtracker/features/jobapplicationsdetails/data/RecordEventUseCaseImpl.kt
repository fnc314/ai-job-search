package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.RecordEventUseCase

internal class RecordEventUseCaseImpl(
    private val repository: JobApplicationRepository,
) : RecordEventUseCase {
    override suspend fun invoke(params: RecordEventUseCase.Params): String =
        repository.recordEvent(params.event)
}
