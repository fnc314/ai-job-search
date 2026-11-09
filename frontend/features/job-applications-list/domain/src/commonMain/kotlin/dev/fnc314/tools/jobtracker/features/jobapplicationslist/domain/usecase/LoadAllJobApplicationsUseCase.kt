package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

/**
 * Clean architecture use case loading all job applications for dashboard display.
 *
 * Implements [UseCase] receiving [LoadAllJobApplicationsUseCase.Params] and returning `List<JobApplication>`.
 */
public interface LoadAllJobApplicationsUseCase : UseCase<LoadAllJobApplicationsUseCase.Params, List<JobApplication>> {

    /**
     * Execution parameters for loading job applications.
     *
     * @property forceRefresh Whether to bypass local SQLite cache and force a remote network query from Firestore.
     */
    public data class Params(public val forceRefresh: Boolean = false)
}
