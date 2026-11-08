package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.UpdateApplicationStatusUseCase

public class UpdateApplicationStatusUseCaseImpl(
    private val repository: JobApplicationsDetailsRepository,
) : UpdateApplicationStatusUseCase {
    override suspend fun invoke(params: UpdateApplicationStatusUseCase.Params): JobApplication =
        repository.updateStatus(params.id, params.status, params.date, params.note)
}
