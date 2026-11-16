package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication

internal interface LocalJobApplicationsListDataSource {
    suspend fun getJobApplications(): List<JobApplication>
    suspend fun saveJobApplications(applications: List<JobApplication>): Unit
    suspend fun clearAndSaveJobApplications(applications: List<JobApplication>): Unit
}
