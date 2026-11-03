package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.Company

interface LoadCompaniesUseCase : UseCase<LoadCompaniesUseCase.Params, List<Company>> {
    data class Params(val forceRefresh: Boolean = false)
}
