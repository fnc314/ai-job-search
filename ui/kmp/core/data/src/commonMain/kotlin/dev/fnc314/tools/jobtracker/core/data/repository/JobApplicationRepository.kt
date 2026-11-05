package dev.fnc314.tools.jobtracker.core.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.*

public interface JobApplicationRepository {
    public suspend fun getAllJobApplications(): List<JobApplication>
    public suspend fun getJobApplication(id: String): JobApplication?
    public suspend fun getCompanies(): List<Company>
    public suspend fun saveJobApplication(app: JobApplication): String
    public suspend fun updateStatus(
        id: String,
        status: TrackerStatus,
        date: String?,
        note: String?
    ): JobApplication
    public suspend fun recordEvent(event: TrackerEvent): String
    public suspend fun updateFitScore(id: String, score: Int): JobApplication
}
