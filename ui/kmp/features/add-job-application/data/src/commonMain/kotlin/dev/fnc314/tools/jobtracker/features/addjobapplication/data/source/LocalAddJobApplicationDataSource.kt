package dev.fnc314.tools.jobtracker.features.addjobapplication.data.source

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent

internal interface LocalAddJobApplicationDataSource {
    suspend fun saveJobApplication(application: JobApplication)
    suspend fun recordEvent(event: TrackerEvent)
}
