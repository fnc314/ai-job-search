package dev.fnc314.tools.jobtracker.features.trackeredits.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseHolder
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.LocalTrackerEditsDataSource

@Suppress("UnusedPrivateProperty")
internal class LocalTrackerEditsDataSourceImpl(
    private val databaseHolder: DatabaseHolder? = null,
    private val localDataSource: LocalDataSource? = null,
) : LocalTrackerEditsDataSource {
    private val edits = mutableListOf<TrackerEdit>()

    override suspend fun getPendingEdits(): List<TrackerEdit> = edits.toList()

    override suspend fun savePendingEdits(edits: List<TrackerEdit>) {
        this.edits.clear()
        this.edits.addAll(edits)
    }

    override suspend fun removeEdit(id: String) {
        edits.removeAll { it.id == id }
    }

    override suspend fun updateEdit(edit: TrackerEdit) {
        val index = edits.indexOfFirst { it.id == edit.id }
        if (index >= 0) {
            edits[index] = edit
        } else {
            edits.add(edit)
        }
    }
}
