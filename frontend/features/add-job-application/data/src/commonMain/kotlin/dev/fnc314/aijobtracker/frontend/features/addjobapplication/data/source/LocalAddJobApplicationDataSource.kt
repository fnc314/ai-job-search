package dev.fnc314.aijobtracker.frontend.features.addjobapplication.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEvent

internal interface LocalAddJobApplicationDataSource {
    suspend fun saveJobApplication(application: JobApplication)
    suspend fun recordEvent(event: TrackerEvent)
}
