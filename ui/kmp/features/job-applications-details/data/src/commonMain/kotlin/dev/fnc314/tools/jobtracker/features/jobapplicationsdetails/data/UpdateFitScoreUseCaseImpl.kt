package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateFitScoreUseCase

internal class UpdateFitScoreUseCaseImpl(
    private val repository: JobApplicationRepository,
) : UpdateFitScoreUseCase {
    override suspend fun invoke(params: UpdateFitScoreUseCase.Params): JobApplication =
        repository.updateFitScore(params.id, params.score)
}
