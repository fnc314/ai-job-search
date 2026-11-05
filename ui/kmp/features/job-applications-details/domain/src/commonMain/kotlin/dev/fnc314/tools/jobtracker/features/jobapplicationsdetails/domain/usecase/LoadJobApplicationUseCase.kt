package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

/**
 * Use case querying a specific application entity by its unique slug or UUID.
 */
public interface LoadJobApplicationUseCase : UseCase<LoadJobApplicationUseCase.Params, JobApplication?> {

    /**
     * Parameter carrier for loading an application.
     *
     * @property id Unique application slug or 36-character UUID string.
     */
    public data class Params(public val id: String)
}
