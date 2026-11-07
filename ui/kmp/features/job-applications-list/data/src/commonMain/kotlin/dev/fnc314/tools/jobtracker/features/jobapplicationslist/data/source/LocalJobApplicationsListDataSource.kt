package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

public interface LocalJobApplicationsListDataSource {
    public suspend fun getJobApplications(): List<JobApplication>
    public suspend fun saveJobApplications(applications: List<JobApplication>): Unit
}
