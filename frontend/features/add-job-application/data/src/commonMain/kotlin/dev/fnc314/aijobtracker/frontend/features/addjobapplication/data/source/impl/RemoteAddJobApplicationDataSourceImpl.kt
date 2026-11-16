package dev.fnc314.aijobtracker.frontend.features.addjobapplication.data.source.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEvent
import dev.fnc314.aijobtracker.frontend.core.data.source.RemoteDataSource
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.data.source.RemoteAddJobApplicationDataSource

@Suppress("UnusedPrivateProperty")
internal class RemoteAddJobApplicationDataSourceImpl(
    private val remoteDataSource: RemoteDataSource? = null,
) : RemoteAddJobApplicationDataSource {
    override suspend fun saveJobApplication(application: JobApplication): String = application.id
    override suspend fun recordEvent(event: TrackerEvent): String = event.id
}
