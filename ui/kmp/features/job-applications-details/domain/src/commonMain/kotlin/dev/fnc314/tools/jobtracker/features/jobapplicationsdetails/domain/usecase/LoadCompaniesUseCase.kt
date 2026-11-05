package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.Company

public interface LoadCompaniesUseCase : UseCase<LoadCompaniesUseCase.Params, List<Company>> {
    public data class Params(public val forceRefresh: Boolean = false)
}
