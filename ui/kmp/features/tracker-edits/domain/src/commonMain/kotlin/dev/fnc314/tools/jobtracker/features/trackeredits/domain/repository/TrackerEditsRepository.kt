package dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit

public interface TrackerEditsRepository {
    public suspend fun getPendingEdits(): List<TrackerEdit>
    public suspend fun approveEdit(id: String, note: String?): TrackerEdit
    public suspend fun rejectEdit(id: String, reason: String): TrackerEdit
    public suspend fun expireStaleEdits(): Int
}
