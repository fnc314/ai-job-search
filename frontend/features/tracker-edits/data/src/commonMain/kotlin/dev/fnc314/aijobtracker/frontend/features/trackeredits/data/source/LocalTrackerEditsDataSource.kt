package dev.fnc314.aijobtracker.frontend.features.trackeredits.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit

internal interface LocalTrackerEditsDataSource {
    suspend fun getPendingEdits(): List<TrackerEdit>
    suspend fun savePendingEdits(edits: List<TrackerEdit>)
    suspend fun removeEdit(id: String)
    suspend fun updateEdit(edit: TrackerEdit)
}
