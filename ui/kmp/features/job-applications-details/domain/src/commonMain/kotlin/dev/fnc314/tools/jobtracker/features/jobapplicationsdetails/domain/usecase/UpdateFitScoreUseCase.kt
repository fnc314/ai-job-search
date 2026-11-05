package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

public interface UpdateFitScoreUseCase : UseCase<UpdateFitScoreUseCase.Params, JobApplication> {
    public data class Params(public val id: String, public val score: Int)
}
