package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateApplicationStatusUseCase

internal class UpdateApplicationStatusUseCaseImpl(
    private val repository: JobApplicationRepository,
) : UpdateApplicationStatusUseCase {
    override suspend fun invoke(params: UpdateApplicationStatusUseCase.Params): JobApplication =
        repository.updateStatus(params.id, params.status, params.date, params.note)
}
