package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.UpdateFitScoreUseCase

internal class UpdateFitScoreUseCaseImpl(
  private val repository: JobApplicationDetailsRepository,
) : UpdateFitScoreUseCase {
    override suspend fun invoke(params: UpdateFitScoreUseCase.Params): JobApplication = repository.updateFitScore(params.id, params.score)
}
