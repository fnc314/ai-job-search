package dev.fnc314.tools.jobtracker.domain.repository

import dev.fnc314.tools.jobtracker.data.model.*

interface JobApplicationRepository {
    suspend fun getAllJobApplications(): List<JobApplication>
    suspend fun getJobApplication(id: String): JobApplication?
    suspend fun getCompanies(): List<Company>
    suspend fun saveJobApplication(app: JobApplication): String
    suspend fun updateStatus(
        id: String,
        status: TrackerStatus,
        date: String?,
        note: String?
    ): JobApplication
    suspend fun recordEvent(event: TrackerEvent): String
    suspend fun updateFitScore(id: String, score: Int): JobApplication
}
