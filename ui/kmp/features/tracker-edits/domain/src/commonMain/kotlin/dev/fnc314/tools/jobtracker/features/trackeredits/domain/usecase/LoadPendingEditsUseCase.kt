package dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit

public interface LoadPendingEditsUseCase : UseCase<LoadPendingEditsUseCase.Params, List<TrackerEdit>> {
    public data class Params(public val forceRefresh: Boolean = false)
}
