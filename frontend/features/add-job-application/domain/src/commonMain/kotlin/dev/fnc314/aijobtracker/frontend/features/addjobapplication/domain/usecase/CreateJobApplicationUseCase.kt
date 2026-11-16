package dev.fnc314.aijobtracker.frontend.features.addjobapplication.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase
import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication

/**
 * Use case saving a newly drafted or submitted job application into the tracking pipeline.
 */
public fun interface CreateJobApplicationUseCase : UseCase<CreateJobApplicationUseCase.Params, String> {

    /**
     * Parameter carrier containing the [JobApplication] to persist.
     *
     * @property application The fully initialized application entity.
     */
    public data class Params(public val application: JobApplication)
}
