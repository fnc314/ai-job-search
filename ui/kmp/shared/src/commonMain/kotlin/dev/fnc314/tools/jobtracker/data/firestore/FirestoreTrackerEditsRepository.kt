package dev.fnc314.tools.jobtracker.data.firestore

import dev.fnc314.tools.jobtracker.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.domain.repository.TrackerEditsRepository

internal class FirestoreTrackerEditsRepository : TrackerEditsRepository {

    override suspend fun getPendingEdits(): List<TrackerEdit> {
        // TODO: Implement Firestore query
        return emptyList()
    }

    override suspend fun approveEdit(id: String, note: String?): TrackerEdit {
        // TODO: Implement Firestore update
        throw NotImplementedError()
    }

    override suspend fun rejectEdit(id: String, reason: String): TrackerEdit {
        // TODO: Implement Firestore update
        throw NotImplementedError()
    }

    override suspend fun expireStaleEdits(): Int {
        // TODO: Implement Firestore query and update
        return 0
    }
}
