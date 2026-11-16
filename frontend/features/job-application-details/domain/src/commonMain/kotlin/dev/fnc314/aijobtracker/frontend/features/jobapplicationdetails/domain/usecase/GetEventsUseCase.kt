package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEvent
import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase

/**
 * Use case fetching all audit events for a specific application from its timeline subcollection.
 */
public fun interface GetEventsUseCase : UseCase<GetEventsUseCase.Params, List<TrackerEvent>> {

    /**
     * Parameter carrier for loading events.
     *
     * @property applicationId Unique application slug or UUID.
     */
    public data class Params(public val applicationId: String)
}
