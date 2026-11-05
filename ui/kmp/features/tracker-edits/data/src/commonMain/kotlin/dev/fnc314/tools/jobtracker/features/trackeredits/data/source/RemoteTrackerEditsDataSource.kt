package dev.fnc314.tools.jobtracker.features.trackeredits.data.source

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEditState
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource

public interface RemoteTrackerEditsDataSource {
    public suspend fun getPendingEdits(): List<TrackerEdit>
    public suspend fun approveEdit(id: String, note: String?): TrackerEdit
    public suspend fun rejectEdit(id: String, reason: String): TrackerEdit
    public suspend fun expireStaleEdits(): Int
}

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
}
