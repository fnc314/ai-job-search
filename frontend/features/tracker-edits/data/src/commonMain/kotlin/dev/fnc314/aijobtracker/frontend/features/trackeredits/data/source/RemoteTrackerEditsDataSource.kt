package dev.fnc314.aijobtracker.frontend.features.trackeredits.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEventType
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus

internal interface RemoteTrackerEditsDataSource {
    suspend fun getPendingEdits(): List<TrackerEdit>
    suspend fun approveEdit(id: String, note: String?): TrackerEdit
    suspend fun rejectEdit(id: String, reason: String): TrackerEdit
    suspend fun expireStaleEdits(): Int
    suspend fun editProposedChange(
        proposalId: String,
        itemIndex: Int,
        newEventType: TrackerEventType,
        newStatusTo: TrackerStatus?,
        reason: String?
    ): TrackerEdit
}
