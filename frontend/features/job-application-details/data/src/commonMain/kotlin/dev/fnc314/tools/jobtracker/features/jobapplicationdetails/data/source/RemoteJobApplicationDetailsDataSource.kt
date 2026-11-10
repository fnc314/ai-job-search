package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus

internal interface RemoteJobApplicationDetailsDataSource {
    suspend fun getJobApplication(id: String): JobApplication?
    suspend fun getCompanies(): List<Company>
    suspend fun getEvents(applicationId: String): List<TrackerEvent>
    suspend fun getArtifacts(applicationId: String): List<TrackerArtifact>
    suspend fun updateStatus(id: String, status: TrackerStatus, date: String?, note: String?): JobApplication
    suspend fun recordEvent(event: TrackerEvent): String
    suspend fun updateFitScore(id: String, score: Int): JobApplication
}
