package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

internal interface LocalJobApplicationsListDataSource {
    suspend fun getJobApplications(): List<JobApplication>
    suspend fun saveJobApplications(applications: List<JobApplication>): Unit
    suspend fun clearAndSaveJobApplications(applications: List<JobApplication>): Unit
}
