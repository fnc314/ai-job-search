package dev.fnc314.tools.jobtracker.features.addjobapplication.data.source

import dev.fnc314.tools.jobtracker.core.data.database.AppDatabase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource

public interface LocalAddJobApplicationDataSource {
    public suspend fun saveJobApplication(application: JobApplication): Unit
    public suspend fun recordEvent(event: TrackerEvent): Unit
}

internal class LocalAddJobApplicationDataSourceImpl(
    private val localDataSource: LocalDataSource? = null,
    private val appDatabase: AppDatabase? = null,
) : LocalAddJobApplicationDataSource {
    private val applications = mutableListOf<JobApplication>()
    private val events = mutableListOf<TrackerEvent>()

    override suspend fun saveJobApplication(application: JobApplication): Unit {
        applications.add(application)
    }

    override suspend fun recordEvent(event: TrackerEvent): Unit {
        events.add(event)
    }
}
