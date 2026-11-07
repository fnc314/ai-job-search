package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseHolder
import dev.fnc314.tools.jobtracker.core.data.database.JobApplicationMapper
import dev.fnc314.tools.jobtracker.core.data.database.dao.JobApplicationDao
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.LocalJobApplicationsListDataSource

internal class LocalJobApplicationsListDataSourceImpl(
  private val databaseHolder: DatabaseHolder? = null,
  private val localDataSource: LocalDataSource? = null,
) : LocalJobApplicationsListDataSource {
  private val memoryCache = mutableListOf<JobApplication>()

  private val dao: JobApplicationDao?
    get() = databaseHolder?.dao

  override suspend fun getJobApplications(): List<JobApplication> {
    val dbDao = dao
    if (dbDao != null) {
      val entities = runCatching { dbDao.getAll() }.getOrNull()
      if (!entities.isNullOrEmpty()) {
        return JobApplicationMapper.toDomainList(entities)
      }
    }
    return memoryCache.toList()
  }

  override suspend fun saveJobApplications(applications: List<JobApplication>): Unit {
    memoryCache.clear()
    memoryCache.addAll(applications)
    val dbDao = dao
    if (dbDao != null) {
      runCatching {
        dbDao.insertAll(JobApplicationMapper.toEntityList(applications))
      }
    }
  }
}