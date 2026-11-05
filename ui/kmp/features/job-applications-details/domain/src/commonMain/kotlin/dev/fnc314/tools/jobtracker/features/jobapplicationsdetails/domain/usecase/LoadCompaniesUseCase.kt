package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.Company

/**
 * Use case retrieving the collection of registered employer companies for auto-completion and company cards.
 */
public interface LoadCompaniesUseCase : UseCase<LoadCompaniesUseCase.Params, List<Company>> {

    /**
     * Parameter carrier for querying companies.
     *
     * @property forceRefresh True to force a remote network query from Firestore.
     */
    public data class Params(public val forceRefresh: Boolean = false)
}
