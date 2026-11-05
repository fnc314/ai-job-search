package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

interface UpdateFitScoreUseCase : UseCase<UpdateFitScoreUseCase.Params, JobApplication> {
    data class Params(val id: String, val score: Int)
}
