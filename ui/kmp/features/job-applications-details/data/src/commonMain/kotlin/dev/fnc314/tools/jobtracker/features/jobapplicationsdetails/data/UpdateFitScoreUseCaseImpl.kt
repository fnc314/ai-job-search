package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateFitScoreUseCase

public class UpdateFitScoreUseCaseImpl(
    private val repository: JobApplicationsDetailsRepository,
) : UpdateFitScoreUseCase {
    override suspend fun invoke(params: UpdateFitScoreUseCase.Params): JobApplication =
        repository.updateFitScore(params.id, params.score)
}
