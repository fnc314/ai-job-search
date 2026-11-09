package dev.fnc314.tools.jobtracker.features.trackeredits.data.source

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit

internal interface RemoteTrackerEditsDataSource {
    suspend fun getPendingEdits(): List<TrackerEdit>
    suspend fun approveEdit(id: String, note: String?): TrackerEdit
    suspend fun rejectEdit(id: String, reason: String): TrackerEdit
    suspend fun expireStaleEdits(): Int
}
