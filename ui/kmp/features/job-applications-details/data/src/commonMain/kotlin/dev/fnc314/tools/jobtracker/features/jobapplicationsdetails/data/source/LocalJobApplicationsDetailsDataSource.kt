package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source

import dev.fnc314.tools.jobtracker.core.data.database.AppDatabase
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource

public interface LocalJobApplicationsDetailsDataSource {
    public suspend fun getJobApplication(id: String): JobApplication?
    public suspend fun saveJobApplication(application: JobApplication): Unit
    public suspend fun getCompanies(): List<Company>
    public suspend fun saveCompanies(companies: List<Company>): Unit
}

internal class LocalJobApplicationsDetailsDataSourceImpl(
    private val localDataSource: LocalDataSource? = null,
    private val appDatabase: AppDatabase? = null,
) : LocalJobApplicationsDetailsDataSource {
    private val applicationCache = mutableMapOf<String, JobApplication>()
    private val companyCache = mutableListOf<Company>()

    override suspend fun getJobApplication(id: String): JobApplication? = applicationCache[id]

    override suspend fun saveJobApplication(application: JobApplication): Unit {
        applicationCache[application.id] = application
    }

    override suspend fun getCompanies(): List<Company> = companyCache.toList()

    override suspend fun saveCompanies(companies: List<Company>): Unit {
        companyCache.clear()
        companyCache.addAll(companies)
    }
}
