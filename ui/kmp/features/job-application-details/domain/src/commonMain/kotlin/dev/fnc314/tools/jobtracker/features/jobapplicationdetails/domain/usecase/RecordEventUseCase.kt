package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

/**
 * Use case appending an immutable audit event to an application's event subcollection.
 */
public interface RecordEventUseCase : UseCase<RecordEventUseCase.Params, String> {

    /**
     * Parameter carrier containing the [TrackerEvent] to persist.
     *
     * @property event The audit event model.
     */
    public data class Params(public val event: TrackerEvent)
}
