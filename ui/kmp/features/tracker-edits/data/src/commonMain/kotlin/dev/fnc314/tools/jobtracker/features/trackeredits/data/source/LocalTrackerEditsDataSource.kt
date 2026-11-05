package dev.fnc314.tools.jobtracker.features.trackeredits.data.source

import dev.fnc314.tools.jobtracker.core.data.database.AppDatabase
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource

public interface LocalTrackerEditsDataSource {
    public suspend fun getPendingEdits(): List<TrackerEdit>
    public suspend fun savePendingEdits(edits: List<TrackerEdit>): Unit
    public suspend fun removeEdit(id: String): Unit
    public suspend fun updateEdit(edit: TrackerEdit): Unit
}

internal class LocalTrackerEditsDataSourceImpl(
    private val localDataSource: LocalDataSource? = null,
    private val appDatabase: AppDatabase? = null,
) : LocalTrackerEditsDataSource {
    private val edits = mutableListOf<TrackerEdit>()

    override suspend fun getPendingEdits(): List<TrackerEdit> = edits.toList()

    override suspend fun savePendingEdits(edits: List<TrackerEdit>): Unit {
        this.edits.clear()
        this.edits.addAll(edits)
    }

    override suspend fun removeEdit(id: String): Unit {
        edits.removeAll { it.id == id }
    }

    override suspend fun updateEdit(edit: TrackerEdit): Unit {
        val index = edits.indexOfFirst { it.id == edit.id }
        if (index >= 0) {
            edits[index] = edit
        } else {
            edits.add(edit)
        }
    }
}
