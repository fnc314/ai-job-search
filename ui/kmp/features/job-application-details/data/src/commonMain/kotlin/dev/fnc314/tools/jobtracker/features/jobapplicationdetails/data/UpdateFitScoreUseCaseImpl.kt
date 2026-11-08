package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.UpdateFitScoreUseCase

public class UpdateFitScoreUseCaseImpl(
    private val repository: JobApplicationsDetailsRepository,
) : UpdateFitScoreUseCase {
    override suspend fun invoke(params: UpdateFitScoreUseCase.Params): JobApplication =
        repository.updateFitScore(params.id, params.score)
}
