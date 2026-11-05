package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

/**
 * Use case updating an application's alignment rating score.
 */
public interface UpdateFitScoreUseCase : UseCase<UpdateFitScoreUseCase.Params, JobApplication> {

    /**
     * Parameter carrier for fit score updates.
     *
     * @property id Target application slug or UUID identifier.
     * @property score New fit score rating (0..100).
     */
    public data class Params(public val id: String, public val score: Int)
}
