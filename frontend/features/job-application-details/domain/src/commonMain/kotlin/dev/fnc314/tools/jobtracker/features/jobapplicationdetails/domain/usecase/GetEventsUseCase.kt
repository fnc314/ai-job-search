package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

/**
 * Use case fetching all audit events for a specific application from its timeline subcollection.
 */
public interface GetEventsUseCase : UseCase<GetEventsUseCase.Params, List<TrackerEvent>> {

    /**
     * Parameter carrier for loading events.
     *
     * @property applicationId Unique application slug or UUID.
     */
    public data class Params(public val applicationId: String)
}
