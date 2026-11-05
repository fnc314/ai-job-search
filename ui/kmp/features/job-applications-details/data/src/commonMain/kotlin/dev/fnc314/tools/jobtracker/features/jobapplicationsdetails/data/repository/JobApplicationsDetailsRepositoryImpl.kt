package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source.LocalJobApplicationsDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source.NetworkJobApplicationsDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source.RemoteJobApplicationsDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.repository.JobApplicationsDetailsRepository

internal class JobApplicationsDetailsRepositoryImpl(
    private val remoteDataSource: RemoteJobApplicationsDetailsDataSource,
    private val networkDataSource: NetworkJobApplicationsDetailsDataSource,
    private val localDataSource: LocalJobApplicationsDetailsDataSource,
) : JobApplicationsDetailsRepository {

    override suspend fun getJobApplication(id: String): JobApplication? {
        val cached = localDataSource.getJobApplication(id)
        if (cached != null) return cached
        val remote = remoteDataSource.getJobApplication(id)
        if (remote != null) {
            localDataSource.saveJobApplication(remote)
        }
        return remote
    }

    override suspend fun getCompanies(): List<Company> {
        val cached = localDataSource.getCompanies()
        if (cached.isNotEmpty()) return cached
        val remote = remoteDataSource.getCompanies()
        if (remote.isNotEmpty()) {
            localDataSource.saveCompanies(remote)
        }
        return remote
    }

    override suspend fun updateStatus(
        id: String,
        status: TrackerStatus,
        date: String?,
        note: String?,
    ): JobApplication {
        val updated = remoteDataSource.updateStatus(id, status, date, note)
        localDataSource.saveJobApplication(updated)
        return updated
    }

    override suspend fun recordEvent(event: TrackerEvent): String {
        return remoteDataSource.recordEvent(event)
    }

    override suspend fun updateFitScore(id: String, score: Int): JobApplication {
        val updated = remoteDataSource.updateFitScore(id, score)
        localDataSource.saveJobApplication(updated)
        return updated
    }

    override suspend fun getArtifactSignedUrl(path: String): String {
        return networkDataSource.getArtifactSignedUrl(path)
    }
}
