package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source

import dev.fnc314.tools.jobtracker.core.data.database.AppDatabase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource

public interface LocalJobApplicationsListDataSource {
    public suspend fun getJobApplications(): List<JobApplication>
    public suspend fun saveJobApplications(applications: List<JobApplication>): Unit
}

internal class LocalJobApplicationsListDataSourceImpl(
    private val localDataSource: LocalDataSource? = null,
    private val appDatabase: AppDatabase? = null,
) : LocalJobApplicationsListDataSource {
    private val memoryCache = mutableListOf<JobApplication>()

    override suspend fun getJobApplications(): List<JobApplication> {
        return memoryCache.toList()
    }

    override suspend fun saveJobApplications(applications: List<JobApplication>): Unit {
        memoryCache.clear()
        memoryCache.addAll(applications)
    }
}
