package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.RecordEventUseCase

public class RecordEventUseCaseImpl(
    private val repository: JobApplicationsDetailsRepository,
) : RecordEventUseCase {
    override suspend fun invoke(params: RecordEventUseCase.Params): String =
        repository.recordEvent(params.event)
}
