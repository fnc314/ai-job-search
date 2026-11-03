package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.TrackerEdit

interface LoadPendingEditsUseCase : UseCase<LoadPendingEditsUseCase.Params, List<TrackerEdit>> {
    data class Params(val forceRefresh: Boolean = false)
}
