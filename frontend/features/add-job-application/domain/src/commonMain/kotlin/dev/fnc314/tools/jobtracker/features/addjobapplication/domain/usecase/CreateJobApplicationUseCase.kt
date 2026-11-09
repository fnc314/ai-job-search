package dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

/**
 * Use case saving a newly drafted or submitted job application into the tracking pipeline.
 */
public interface CreateJobApplicationUseCase : UseCase<CreateJobApplicationUseCase.Params, String> {

    /**
     * Parameter carrier containing the [JobApplication] to persist.
     *
     * @property application The fully initialized application entity.
     */
    public data class Params(public val application: JobApplication)
}
