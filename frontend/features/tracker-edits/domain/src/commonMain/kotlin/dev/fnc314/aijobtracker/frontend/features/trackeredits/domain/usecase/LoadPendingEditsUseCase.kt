package dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit

/**
 * Use case loading all edit proposals awaiting candidate review.
 */
public fun interface LoadPendingEditsUseCase : UseCase<LoadPendingEditsUseCase.Params, List<TrackerEdit>> {

    /**
     * Parameter carrier for querying pending edits.
     *
     * @property forceRefresh Whether to force a remote query bypassing local cache.
     */
    public data class Params(public val forceRefresh: Boolean = false)
}
