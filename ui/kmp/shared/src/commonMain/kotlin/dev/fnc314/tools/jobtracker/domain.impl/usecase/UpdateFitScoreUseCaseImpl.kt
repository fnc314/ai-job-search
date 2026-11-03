package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.domain.usecase.UpdateFitScoreUseCase

internal class UpdateFitScoreUseCaseImpl(
    private val repository: JobApplicationRepository,
) : UpdateFitScoreUseCase {
    override suspend fun invoke(params: UpdateFitScoreUseCase.Params): JobApplication =
        repository.updateFitScore(params.id, params.score)
}
