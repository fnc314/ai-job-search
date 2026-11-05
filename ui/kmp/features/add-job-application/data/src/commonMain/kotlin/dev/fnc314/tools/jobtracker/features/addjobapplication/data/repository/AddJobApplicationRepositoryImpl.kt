package dev.fnc314.tools.jobtracker.features.addjobapplication.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.LocalAddJobApplicationDataSource
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.RemoteAddJobApplicationDataSource
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.repository.AddJobApplicationRepository

internal class AddJobApplicationRepositoryImpl(
    private val remoteDataSource: RemoteAddJobApplicationDataSource,
    private val localDataSource: LocalAddJobApplicationDataSource,
) : AddJobApplicationRepository {

    override suspend fun saveJobApplication(application: JobApplication): String {
        val id = remoteDataSource.saveJobApplication(application)
        localDataSource.saveJobApplication(application)
        return id
    }

    override suspend fun recordEvent(event: TrackerEvent): String {
        val id = remoteDataSource.recordEvent(event)
        localDataSource.recordEvent(event)
        return id
    }
}
