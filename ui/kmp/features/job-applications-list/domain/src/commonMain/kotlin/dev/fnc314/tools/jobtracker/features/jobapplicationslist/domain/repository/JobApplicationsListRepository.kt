package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.repository

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

public interface JobApplicationsListRepository {
    public suspend fun getJobApplications(): List<JobApplication>
}
