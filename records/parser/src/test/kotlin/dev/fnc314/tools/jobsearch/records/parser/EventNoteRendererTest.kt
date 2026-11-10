package dev.fnc314.tools.jobsearch.records.parser

import com.google.protobuf.Timestamp
import dev.fnc314.tools.jobsearch.parser.ApplicationTimelineProjector
import dev.fnc314.tools.jobsearch.parser.EventNoteRenderer
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventNoteRendererTest {

    @Test
    fun testRenderStandardEventNote() {
        val event = TrackerEvent.newBuilder()
            .setId("ev-1")
            .setType(TrackerEventType.APPLY)
            .setNote("Sent application via careers portal")
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776000).build()) // 2025-11-10
            .build()

        val rendered = EventNoteRenderer.renderNote(event)
        assertEquals("2025-11-10 · APPLY — Sent application via careers portal", rendered)
    }

    @Test
    fun testRenderImportEventOmitsPrefix() {
        val event = TrackerEvent.newBuilder()
            .setId("ev-import")
            .setType(TrackerEventType.IMPORT)
            .setNote("Imported legacy notes from spreadsheet")
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776000).build())
            .build()

        val rendered = EventNoteRenderer.renderNote(event)
        assertEquals("Imported legacy notes from spreadsheet", rendered)
    }

    @Test
    fun testRenderNotesChronological() {
        val ev1 = TrackerEvent.newBuilder()
            .setId("ev-1")
            .setType(TrackerEventType.APPLY)
            .setNote("Applied")
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776000).build())
            .setSequenceNumber(1)
            .build()

        val ev2 = TrackerEvent.newBuilder()
            .setId("ev-2")
            .setType(TrackerEventType.SCHEDULE_INTERVIEW)
            .setNote("First round scheduled")
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776500).build())
            .setSequenceNumber(2)
            .build()

        val rendered = EventNoteRenderer.renderNotes(listOf(ev2, ev1))
        val expected = "2025-11-10 · APPLY — Applied\n2025-11-10 · SCHEDULE_INTERVIEW — First round scheduled"
        assertEquals(expected, rendered)
    }

    @Test
    fun testRenderNotesEmptyReturnsNull() {
        assertNull(EventNoteRenderer.renderNotes(emptyList()))
    }

    @Test
    fun testProjectLatestStatus() {
        val ev1 = TrackerEvent.newBuilder()
            .setId("ev-1")
            .setType(TrackerEventType.APPLY)
            .setStatusTo(TrackerStatus.APPLIED)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776000).build())
            .setSequenceNumber(1)
            .build()

        val ev2 = TrackerEvent.newBuilder()
            .setId("ev-2")
            .setType(TrackerEventType.REJECT)
            .setStatusTo(TrackerStatus.REJECTED)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776500).build())
            .setSequenceNumber(2)
            .build()

        val status = ApplicationTimelineProjector.latestStatus(listOf(ev1, ev2))
        assertEquals(TrackerStatus.REJECTED, status)
    }
}
