package dev.fnc314.aijobtracker.frontend.features.trackeredits.data.source.impl

import dev.fnc314.aijobtracker.frontend.core.data.factory.Factory
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEditState
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEventType
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.core.data.source.RemoteDataSource
import dev.fnc314.aijobtracker.frontend.features.trackeredits.data.source.RemoteTrackerEditsDataSource

@Suppress("UnusedPrivateProperty")
internal class RemoteTrackerEditsDataSourceImpl(
    private val remoteDataSource: RemoteDataSource? = null,
) : RemoteTrackerEditsDataSource {
    override suspend fun getPendingEdits(): List<TrackerEdit> = emptyList()

    override suspend fun approveEdit(id: String, note: String?): TrackerEdit {
        return Factory.createTrackerEdit(
            id = id,
            state = TrackerEditState.APPROVED,
        )
    }

    override suspend fun rejectEdit(id: String, reason: String): TrackerEdit {
        return Factory.createTrackerEdit(
            id = id,
            state = TrackerEditState.REJECTED,
        )
    }

    override suspend fun expireStaleEdits(): Int = 0

    override suspend fun editProposedChange(
        proposalId: String,
        itemIndex: Int,
        newEventType: TrackerEventType,
        newStatusTo: TrackerStatus?,
        reason: String?
    ): TrackerEdit {
        return Factory.createTrackerEdit(
            id = proposalId,
            state = TrackerEditState.PENDING,
        )
    }
}
