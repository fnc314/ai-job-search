package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.JobApplication

interface UpdateFitScoreUseCase : UseCase<UpdateFitScoreUseCase.Params, JobApplication> {
    data class Params(val id: String, val score: Int)
}
