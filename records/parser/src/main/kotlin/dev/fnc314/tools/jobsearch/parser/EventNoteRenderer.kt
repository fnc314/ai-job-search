package dev.fnc314.tools.jobsearch.parser

import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import java.time.Instant

/**
 * Pure Kotlin renderer converting append-only event logs into canonical human-readable notes.
 *
 * Implements the formatting rules previously defined in `scripts/render.py`:
 * - Standard event lines format as: `"<date> · <type> — <note>"`
 * - `event:imported` / [TrackerEventType.IMPORT] displays note prose directly without prefix decoration.
 */
public object EventNoteRenderer {

    public const val NOTE_SEPARATOR: String = " · "
    public const val NOTE_DASH: String = " — "

    /**
     * Formats an individual [TrackerEvent] into a single-line note string.
     */
    public fun renderNote(event: TrackerEvent): String {
        val note = event.note.trim()
        if (event.type == TrackerEventType.IMPORT) {
            return note
        }

        val dateStr = if (event.hasCreatedAt() && event.createdAt.seconds > 0) {
            Instant.ofEpochSecond(event.createdAt.seconds, event.createdAt.nanos.toLong())
                .toString()
                .substringBefore("T")
        } else {
            ""
        }

        val typeStr = if (event.type != TrackerEventType.TRACKER_EVENT_TYPE_UNSPECIFIED) {
            event.type.name
        } else {
            "NOTE_ADDED"
        }

        val prefix = if (dateStr.isNotBlank()) {
            "$dateStr$NOTE_SEPARATOR$typeStr"
        } else {
            typeStr
        }

        return if (note.isNotBlank()) {
            "$prefix$NOTE_DASH$note"
        } else {
            prefix
        }
    }

    /**
     * Formats a collection of [TrackerEvent]s into a chronological multi-line history string,
     * ordered oldest first. Returns `null` if the collection produces no non-blank lines.
     */
    public fun renderNotes(events: List<TrackerEvent>): String? {
        if (events.isEmpty()) return null

        val sorted = events.sortedWith(
            compareBy<TrackerEvent> { if (it.sequenceNumber > 0) it.sequenceNumber else Long.MAX_VALUE }
                .thenBy { it.createdAt.seconds }
                .thenBy { it.id }
        )

        val lines = sorted.map { renderNote(it).trim() }
            .filter { it.isNotBlank() }

        return if (lines.isEmpty()) null else lines.joinToString("\n")
    }
}
