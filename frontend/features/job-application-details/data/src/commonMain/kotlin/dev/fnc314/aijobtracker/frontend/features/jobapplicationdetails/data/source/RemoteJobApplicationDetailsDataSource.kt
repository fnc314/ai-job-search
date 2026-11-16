package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.Company
import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication
import dev.fnc314.aijobtracker.frontend.core.artifacts.model.TrackerArtifact
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEvent
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus

internal interface RemoteJobApplicationDetailsDataSource {
    suspend fun getJobApplication(id: String): JobApplication?
    suspend fun getCompanies(): List<Company>
    suspend fun getEvents(applicationId: String): List<TrackerEvent>
    suspend fun getArtifacts(applicationId: String): List<TrackerArtifact>
    suspend fun updateStatus(id: String, status: TrackerStatus, date: String?, note: String?): JobApplication
    suspend fun recordEvent(event: TrackerEvent): String
}
