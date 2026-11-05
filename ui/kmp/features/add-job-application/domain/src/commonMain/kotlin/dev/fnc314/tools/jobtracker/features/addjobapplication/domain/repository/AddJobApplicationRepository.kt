package dev.fnc314.tools.jobtracker.features.addjobapplication.domain.repository

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent

/**
 * Domain repository contract persisting new applications and unsubmitted leads into the pipeline.
 */
public interface AddJobApplicationRepository {

    /**
     * Persists a newly created job application, writing to remote storage and local cache.
     *
     * @param application The [JobApplication] entity to save.
     * @return The slug identifier of the saved application.
     */
    public suspend fun saveJobApplication(application: JobApplication): String

    /**
     * Appends an audit event to a newly established application timeline.
     *
     * @param event The [TrackerEvent] to record.
     * @return The generated event identifier.
     */
    public suspend fun recordEvent(event: TrackerEvent): String
}
