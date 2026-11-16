package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.model.Company
import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase

/**
 * Use case retrieving the collection of registered employer companies for auto-completion and company cards.
 */
public fun interface LoadCompaniesUseCase : UseCase<LoadCompaniesUseCase.Params, List<Company>> {

    /**
     * Parameter carrier for querying companies.
     *
     * @property forceRefresh True to force a remote network query from Firestore.
     */
    public data class Params(public val forceRefresh: Boolean = false)
}
