package dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseHolder
import dev.fnc314.tools.jobtracker.core.data.database.JobApplicationMapper
import dev.fnc314.tools.jobtracker.core.data.database.dao.JobApplicationDao
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.LocalAddJobApplicationDataSource

@Suppress("UnusedPrivateProperty")
internal class LocalAddJobApplicationDataSourceImpl(
    private val databaseHolder: DatabaseHolder? = null,
    private val localDataSource: LocalDataSource? = null,
) : LocalAddJobApplicationDataSource {
    private val applications = mutableListOf<JobApplication>()
    private val events = mutableListOf<TrackerEvent>()

    private val dao: JobApplicationDao?
        get() = databaseHolder?.dao

    override suspend fun saveJobApplication(application: JobApplication) {
        applications.add(application)
        val dbDao = dao
        if (dbDao != null) {
            runCatching {
                dbDao.insert(JobApplicationMapper.toEntity(application))
            }
        }
    }

    override suspend fun recordEvent(event: TrackerEvent) {
        events.add(event)
    }
}
