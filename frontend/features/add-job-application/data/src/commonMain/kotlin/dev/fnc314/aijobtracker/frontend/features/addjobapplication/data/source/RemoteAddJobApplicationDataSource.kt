package dev.fnc314.aijobtracker.frontend.features.addjobapplication.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEvent

internal interface RemoteAddJobApplicationDataSource {
    suspend fun saveJobApplication(application: JobApplication): String
    suspend fun recordEvent(event: TrackerEvent): String
}
