package dev.fnc314.tools.jobtracker.features.trackeredits.data.repository.impl

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.LocalTrackerEditsDataSource
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.RemoteTrackerEditsDataSource
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository.TrackerEditsRepository

internal class TrackerEditsRepositoryImpl(
    private val remoteDataSource: RemoteTrackerEditsDataSource,
    private val localDataSource: LocalTrackerEditsDataSource,
) : TrackerEditsRepository {

    override suspend fun getPendingEdits(): List<TrackerEdit> {
        val cached = localDataSource.getPendingEdits()
        if (cached.isNotEmpty()) return cached
        val remote = remoteDataSource.getPendingEdits()
        if (remote.isNotEmpty()) {
            localDataSource.savePendingEdits(remote)
        }
        return remote
    }

    override suspend fun approveEdit(id: String, note: String?): TrackerEdit {
        val updated = remoteDataSource.approveEdit(id, note)
        localDataSource.removeEdit(id)
        return updated
    }

    override suspend fun rejectEdit(id: String, reason: String): TrackerEdit {
        val updated = remoteDataSource.rejectEdit(id, reason)
        localDataSource.removeEdit(id)
        return updated
    }

    override suspend fun expireStaleEdits(): Int {
        val expiredCount = remoteDataSource.expireStaleEdits()
        val remaining = remoteDataSource.getPendingEdits()
        localDataSource.savePendingEdits(remaining)
        return expiredCount
    }
}
