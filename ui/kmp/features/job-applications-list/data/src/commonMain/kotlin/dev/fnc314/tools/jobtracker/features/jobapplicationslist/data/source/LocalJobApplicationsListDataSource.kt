package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

internal interface LocalJobApplicationsListDataSource {
    public suspend fun getJobApplications(): List<JobApplication>
    public suspend fun saveJobApplications(applications: List<JobApplication>): Unit
    public suspend fun clearAndSaveJobApplications(applications: List<JobApplication>): Unit
}
