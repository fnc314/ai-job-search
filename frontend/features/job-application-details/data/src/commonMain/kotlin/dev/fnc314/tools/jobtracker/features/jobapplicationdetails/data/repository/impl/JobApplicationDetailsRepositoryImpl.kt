package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.repository.impl

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.LocalJobApplicationDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.NetworkJobApplicationDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.RemoteJobApplicationDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository

internal class JobApplicationDetailsRepositoryImpl(
    private val remoteDataSource: RemoteJobApplicationDetailsDataSource,
    private val networkDataSource: NetworkJobApplicationDetailsDataSource,
    private val localDataSource: LocalJobApplicationDetailsDataSource,
) : JobApplicationDetailsRepository {

    override suspend fun getJobApplication(id: String): JobApplication? {
        val cached = runCatching { localDataSource.getJobApplication(id) }.getOrNull()
        if (cached != null) return cached
        val remote = runCatching { remoteDataSource.getJobApplication(id) }.getOrNull()
        if (remote != null) {
            runCatching { localDataSource.saveJobApplication(remote) }
        }
        return remote
    }

    override suspend fun getCompanies(): List<Company> {
        val cached = runCatching { localDataSource.getCompanies() }.getOrDefault(emptyList())
        if (cached.isNotEmpty()) return cached
        val remote = runCatching { remoteDataSource.getCompanies() }.getOrDefault(emptyList())
        if (remote.isNotEmpty()) {
            runCatching { localDataSource.saveCompanies(remote) }
        }
        return remote
    }

    override suspend fun getEvents(applicationId: String): List<TrackerEvent> {
        return runCatching { remoteDataSource.getEvents(applicationId) }.getOrDefault(emptyList())
    }

    override suspend fun updateStatus(
        id: String,
        status: TrackerStatus,
        date: String?,
        note: String?,
    ): JobApplication {
        val updated = remoteDataSource.updateStatus(id, status, date, note)
        runCatching { localDataSource.saveJobApplication(updated) }
        return updated
    }

    override suspend fun recordEvent(event: TrackerEvent): String {
        return remoteDataSource.recordEvent(event)
    }

    override suspend fun updateFitScore(id: String, score: Int): JobApplication {
        val updated = remoteDataSource.updateFitScore(id, score)
        runCatching { localDataSource.saveJobApplication(updated) }
        return updated
    }

    override suspend fun getArtifactSignedUrl(path: String): String {
        return networkDataSource.getArtifactSignedUrl(path)
    }

    override suspend fun getArtifacts(applicationId: String): List<TrackerArtifact> {
        val cached = runCatching { localDataSource.getArtifacts(applicationId) }.getOrNull()
        if (cached != null) return cached
        val remote = runCatching { remoteDataSource.getArtifacts(applicationId) }.getOrDefault(emptyList())
        if (remote.isNotEmpty()) {
            runCatching { localDataSource.saveArtifacts(applicationId, remote) }
        }
        return remote
    }
}
