package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data

import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.RecordEventUseCase

public class RecordEventUseCaseImpl(
    private val repository: JobApplicationsDetailsRepository,
) : RecordEventUseCase {
    override suspend fun invoke(params: RecordEventUseCase.Params): String =
        repository.recordEvent(params.event)
}
