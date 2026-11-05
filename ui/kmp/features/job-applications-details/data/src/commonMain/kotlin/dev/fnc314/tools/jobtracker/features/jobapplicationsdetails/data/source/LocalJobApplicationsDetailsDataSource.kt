package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseHolder
import dev.fnc314.tools.jobtracker.core.data.database.JobApplicationMapper
import dev.fnc314.tools.jobtracker.core.data.database.dao.JobApplicationDao
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
    private val databaseHolder: DatabaseHolder? = null,
    private val localDataSource: LocalDataSource? = null,
) : LocalJobApplicationsDetailsDataSource {
    private val applicationCache = mutableMapOf<String, JobApplication>()
    private val companyCache = mutableListOf<Company>()

    private val dao: JobApplicationDao?
        get() = databaseHolder?.dao

    override suspend fun getJobApplication(id: String): JobApplication? {
        val dbDao = dao
        if (dbDao != null) {
            val entity = runCatching { dbDao.getByIdOrUuid(id) }.getOrNull()
            if (entity != null) {
                return JobApplicationMapper.toDomain(entity)
            }
        }
        return applicationCache[id]
    }

    override suspend fun saveJobApplication(application: JobApplication): Unit {
        applicationCache[application.id] = application
        val dbDao = dao
        if (dbDao != null) {
            runCatching {
                dbDao.insert(JobApplicationMapper.toEntity(application))
            }
        }
    }

    override suspend fun getCompanies(): List<Company> = companyCache.toList()

    override suspend fun saveCompanies(companies: List<Company>): Unit {
        companyCache.clear()
        companyCache.addAll(companies)
    }
}
