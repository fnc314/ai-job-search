package dev.fnc314.tools.jobtracker.features.trackeredits.data

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.core.data.repository.TrackerEditsRepository

class FirestoreTrackerEditsRepository : TrackerEditsRepository {
    override suspend fun getPendingEdits(): List<TrackerEdit> = emptyList()
    override suspend fun approveEdit(id: String, note: String?): TrackerEdit = throw NotImplementedError()
    override suspend fun rejectEdit(id: String, reason: String): TrackerEdit = throw NotImplementedError()
    override suspend fun expireStaleEdits(): Int = 0
}
