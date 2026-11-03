package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase

interface ExpireStaleEditsUseCase : UseCase<ExpireStaleEditsUseCase.Params, Int> {
    data class Params(val forceRefresh: Boolean = false)
}
