package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource

public interface RemoteJobApplicationsListDataSource {
    public suspend fun getJobApplications(): List<JobApplication>
}

internal class RemoteJobApplicationsListDataSourceImpl(
    private val remoteDataSource: RemoteDataSource? = null,
) : RemoteJobApplicationsListDataSource {
    override suspend fun getJobApplications(): List<JobApplication> {
        return emptyList()
    }
}
