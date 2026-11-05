package dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit

interface LoadPendingEditsUseCase : UseCase<LoadPendingEditsUseCase.Params, List<TrackerEdit>> {
    data class Params(val forceRefresh: Boolean = false)
}
