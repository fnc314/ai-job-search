package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.repository

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus

public interface JobApplicationsDetailsRepository {
    public suspend fun getJobApplication(id: String): JobApplication?
    public suspend fun getCompanies(): List<Company>
    public suspend fun updateStatus(id: String, status: TrackerStatus, date: String?, note: String?): JobApplication
    public suspend fun recordEvent(event: TrackerEvent): String
    public suspend fun updateFitScore(id: String, score: Int): JobApplication
    public suspend fun getArtifactSignedUrl(path: String): String
}
