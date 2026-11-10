package dev.fnc314.tools.jobsearch.records.parser

import com.google.protobuf.Timestamp
import dev.fnc314.tools.jobsearch.parser.ApplicationTimelineProjector
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.SubmissionConfidence
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationTimelineProjectorTest {

    @Test
    fun testEmptyTimeline() {
        val tl = ApplicationTimelineProjector.projectTimeline(emptyList())
        assertEquals(0L, tl.submittedAt.seconds)
        assertEquals(0L, tl.submissionConfirmedAt.seconds)
    }

    @Test
    fun testProjectTimelineWithApplyAndAck() {
        val applyEvent = TrackerEvent.newBuilder()
            .setId("ev-apply")
            .setType(TrackerEventType.APPLY)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776000).build())
            .build()

        val ackEvent = TrackerEvent.newBuilder()
            .setId("ev-ack")
            .setType(TrackerEventType.ACKNOWLEDGE)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776300).build())
            .build()

        val evalInstant = Instant.ofEpochSecond(1762776300 + 86400 * 2) // 2 days later
        val tl = ApplicationTimelineProjector.projectTimeline(listOf(applyEvent, ackEvent), evalInstant)

        assertEquals(1762776000L, tl.submittedAt.seconds)
        assertEquals(1762776300L, tl.submissionConfirmedAt.seconds)
        assertEquals(1762776000L, tl.firstActivityAt.seconds)
        assertEquals(1762776300L, tl.lastActivityAt.seconds)
        assertEquals(2, tl.daysSinceActivity)
    }

    @Test
    fun testProjectSubmissionConfirmationScraped() {
        val ackEvent = TrackerEvent.newBuilder()
            .setId("ev-ack")
            .setType(TrackerEventType.ACKNOWLEDGE)
            .setActor(EventActor.ACTOR_EMAIL_SYNC)
            .setEmailMessageId("msg-999")
            .setNote("Thanks for applying")
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776300).build())
            .build()

        val conf = ApplicationTimelineProjector.projectSubmissionConfirmation(
            null,
            listOf(ackEvent)
        )
        assertTrue(conf.isConfirmed)
        assertEquals(SubmissionConfidence.CONFIRMED_SCRAPED, conf.confidence)
        assertEquals("msg-999", conf.confirmationEmailMessageId)
        assertEquals("Thanks for applying", conf.sourceSubject)
    }

    @Test
    fun testProjectSubmissionConfirmationUserMarkedOnly() {
        val applyEvent = TrackerEvent.newBuilder()
            .setId("ev-apply")
            .setType(TrackerEventType.APPLY)
            .setActor(EventActor.ACTOR_USER)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776000).build())
            .build()

        val conf = ApplicationTimelineProjector.projectSubmissionConfirmation(null, listOf(applyEvent))
        assertFalse(conf.isConfirmed)
        assertEquals(SubmissionConfidence.UNCONFIRMED_USER_MARKED, conf.confidence)
    }

    @Test
    fun testProjectCounters() {
        val ev1 = TrackerEvent.newBuilder().setType(TrackerEventType.SCHEDULE_INTERVIEW).build()
        val ev2 = TrackerEvent.newBuilder().setType(TrackerEventType.REJECT).build()
        val ev3 = TrackerEvent.newBuilder().setType(TrackerEventType.SEND_FOLLOW_UP).build()

        val counters = ApplicationTimelineProjector.projectCounters(listOf(ev1, ev2, ev3))
        assertEquals(3, counters["events"])
        assertEquals(1, counters["interviews"])
        assertEquals(1, counters["rejections"])
        assertEquals(1, counters["followUpsSent"])
    }
}
