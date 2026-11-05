package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.LocalJobApplicationsListDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.RemoteJobApplicationsListDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.repository.JobApplicationsListRepository

internal class JobApplicationsListRepositoryImpl(
    private val remoteDataSource: RemoteJobApplicationsListDataSource,
    private val localDataSource: LocalJobApplicationsListDataSource,
) : JobApplicationsListRepository {
    override suspend fun getJobApplications(): List<JobApplication> {
        val cached = runCatching { localDataSource.getJobApplications() }.getOrDefault(emptyList())
        if (cached.isNotEmpty()) {
            return cached
        }
        val remote = runCatching { remoteDataSource.getJobApplications() }.getOrDefault(emptyList())
        if (remote.isNotEmpty()) {
            runCatching { localDataSource.saveJobApplications(remote) }
            return remote
        }
        return cached
    }
}
