package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseHolder
import dev.fnc314.tools.jobtracker.core.data.database.JobApplicationMapper
import dev.fnc314.tools.jobtracker.core.data.database.dao.JobApplicationDao
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.artifacts.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.LocalJobApplicationDetailsDataSource

internal class LocalJobApplicationDetailsDataSourceImpl(
    private val databaseHolder: DatabaseHolder,
) : LocalJobApplicationDetailsDataSource {
    private val applicationCache = mutableMapOf<String, JobApplication>()
    private val companyCache = mutableListOf<Company>()
    private val artifactsCache = mutableMapOf<String, List<TrackerArtifact>>()

    private val dao: JobApplicationDao?
        get() = databaseHolder.dao

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

    override suspend fun saveJobApplication(application: JobApplication) {
        applicationCache[application.id] = application
        val dbDao = dao
        if (dbDao != null) {
            runCatching {
                dbDao.insert(JobApplicationMapper.toEntity(application))
            }
        }
    }

    override suspend fun getCompanies(): List<Company> = companyCache.toList()

    override suspend fun saveCompanies(companies: List<Company>) {
        companyCache.clear()
        companyCache.addAll(companies)
    }

    override suspend fun getArtifacts(applicationId: String): List<TrackerArtifact>? {
        return artifactsCache[applicationId]
    }

    override suspend fun saveArtifacts(applicationId: String, artifacts: List<TrackerArtifact>) {
        artifactsCache[applicationId] = artifacts
    }
}
