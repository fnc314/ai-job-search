package dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

interface ExpireStaleEditsUseCase : UseCase<ExpireStaleEditsUseCase.Params, Int> {
    class Params
}
