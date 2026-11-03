package dev.fnc314.tools.jobtracker.domain.repository

import dev.fnc314.tools.jobtracker.data.model.TrackerEdit

interface TrackerEditsRepository {
    suspend fun getPendingEdits(): List<TrackerEdit>
    suspend fun approveEdit(id: String, note: String?): TrackerEdit
    suspend fun rejectEdit(id: String, reason: String): TrackerEdit
    suspend fun expireStaleEdits(): Int
}
