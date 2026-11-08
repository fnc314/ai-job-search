package dev.fnc314.tools.jobtracker.features.addjobapplication.data.source

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource

internal interface RemoteAddJobApplicationDataSource {
    public suspend fun saveJobApplication(application: JobApplication): String
    public suspend fun recordEvent(event: TrackerEvent): String
}

@Suppress("UnusedPrivateProperty")
internal class RemoteAddJobApplicationDataSourceImpl(
    private val remoteDataSource: RemoteDataSource? = null,
) : RemoteAddJobApplicationDataSource {
    override suspend fun saveJobApplication(application: JobApplication): String = application.id
    override suspend fun recordEvent(event: TrackerEvent): String = event.id
}
