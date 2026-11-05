package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.Company

interface LoadCompaniesUseCase : UseCase<LoadCompaniesUseCase.Params, List<Company>> {
    data class Params(val forceRefresh: Boolean = false)
}
