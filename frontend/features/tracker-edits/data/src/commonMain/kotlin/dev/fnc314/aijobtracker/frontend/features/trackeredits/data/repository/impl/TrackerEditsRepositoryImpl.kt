package dev.fnc314.aijobtracker.frontend.features.trackeredits.data.repository.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEventType
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.features.trackeredits.data.source.LocalTrackerEditsDataSource
import dev.fnc314.aijobtracker.frontend.features.trackeredits.data.source.RemoteTrackerEditsDataSource
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.repository.TrackerEditsRepository

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

    override suspend fun editProposedChange(
        proposalId: String,
        itemIndex: Int,
        newEventType: TrackerEventType,
        newStatusTo: TrackerStatus?,
        reason: String?
    ): TrackerEdit {
        val updated = remoteDataSource.editProposedChange(proposalId, itemIndex, newEventType, newStatusTo, reason)
        localDataSource.savePendingEdits(listOf(updated))
        return updated
    }
}
