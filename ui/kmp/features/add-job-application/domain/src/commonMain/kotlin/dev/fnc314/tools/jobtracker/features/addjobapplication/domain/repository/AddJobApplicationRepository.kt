package dev.fnc314.tools.jobtracker.features.addjobapplication.domain.repository

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent

public interface AddJobApplicationRepository {
    public suspend fun saveJobApplication(application: JobApplication): String
    public suspend fun recordEvent(event: TrackerEvent): String
}
