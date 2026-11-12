package dev.fnc314.tools.jobsearch.records.parser

import com.google.protobuf.Timestamp
import dev.fnc314.tools.jobsearch.parser.EventBuilder
import dev.fnc314.tools.jobsearch.parser.EventError
import dev.fnc314.tools.jobsearch.parser.JobApplicationEventBuilder
import dev.fnc314.tools.jobsearch.tracker.v1.Confidence
import dev.fnc314.tools.jobsearch.tracker.v1.DerivedFrom
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.InterviewRound
import dev.fnc314.tools.jobsearch.tracker.v1.InterviewType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventBuilderTest {

    // === Implied Status ===

    @Test
    fun testImpliedStatusMapsStatusChangingEvents() {
        assertEquals(TrackerStatus.SUBMITTED, EventBuilder.impliedStatus(TrackerEventType.APPLY))
        assertEquals(TrackerStatus.RECRUITER_SCREENED, EventBuilder.impliedStatus(TrackerEventType.RECRUITER_SCREEN_EVENT))
        assertEquals(TrackerStatus.INTERVIEWING, EventBuilder.impliedStatus(TrackerEventType.SCHEDULE_INTERVIEW))
        assertEquals(TrackerStatus.INTERVIEWING, EventBuilder.impliedStatus(TrackerEventType.VISIT_ONSITE))
        assertEquals(TrackerStatus.OFFER, EventBuilder.impliedStatus(TrackerEventType.OFFER_EVENT))
        assertEquals(TrackerStatus.HIRED, EventBuilder.impliedStatus(TrackerEventType.ACCEPT_OFFER))
        assertEquals(TrackerStatus.OFFER_DECLINED, EventBuilder.impliedStatus(TrackerEventType.DECLINE_OFFER))
        assertEquals(TrackerStatus.REJECTED, EventBuilder.impliedStatus(TrackerEventType.REJECT))
        assertEquals(TrackerStatus.NO_RESPONSE, EventBuilder.impliedStatus(TrackerEventType.GHOST))
        assertEquals(TrackerStatus.APPLICATION_WITHDRAWN, EventBuilder.impliedStatus(TrackerEventType.WITHDRAW))
    }

    @Test
    fun testImpliedStatusReturnsNoneForNonStatusEvents() {
        assertNull(EventBuilder.impliedStatus(TrackerEventType.ACKNOWLEDGE))
        assertNull(EventBuilder.impliedStatus(TrackerEventType.COMPLETE_INTERVIEW))
        assertNull(EventBuilder.impliedStatus(TrackerEventType.ASSESS))
        assertNull(EventBuilder.impliedStatus(TrackerEventType.SEND_FOLLOW_UP))
        assertNull(EventBuilder.impliedStatus(TrackerEventType.TAILOR_DOCUMENT))
        assertNull(EventBuilder.impliedStatus(TrackerEventType.RECALCULATE_FIT))
        assertNull(EventBuilder.impliedStatus(TrackerEventType.ADD_ARTIFACT))
        assertNull(EventBuilder.impliedStatus(TrackerEventType.ADD_NOTE))
        assertNull(EventBuilder.impliedStatus(TrackerEventType.IMPORT))
        assertNull(EventBuilder.impliedStatus(TrackerEventType.CORRECT_STATUS))
    }

    // === Latest Status ===

    @Test
    fun testLatestStatusReturnsNullForEmptyList() {
        assertNull(EventBuilder.latestStatus(emptyList()))
    }

    @Test
    fun testLatestStatusReturnsLastStatusBearingEvent() {
        val ev1 = TrackerEvent.newBuilder()
            .setId("ev-001")
            .setType(TrackerEventType.APPLY)
            .setStatusTo(TrackerStatus.SUBMITTED)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1000).build())
            .build()

        val ev2 = TrackerEvent.newBuilder()
            .setId("ev-002")
            .setType(TrackerEventType.ACKNOWLEDGE)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(2000).build())
            .build()

        val ev3 = TrackerEvent.newBuilder()
            .setId("ev-003")
            .setType(TrackerEventType.RECRUITER_SCREEN_EVENT)
            .setStatusTo(TrackerStatus.RECRUITER_SCREENED)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(3000).build())
            .build()

        assertEquals(TrackerStatus.RECRUITER_SCREENED, EventBuilder.latestStatus(listOf(ev1, ev2, ev3)))
    }

    @Test
    fun testLatestStatusIgnoresNonStatusEvents() {
        val ev1 = TrackerEvent.newBuilder()
            .setId("ev-001")
            .setType(TrackerEventType.APPLY)
            .setStatusTo(TrackerStatus.SUBMITTED)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1000).build())
            .build()

        val ev2 = TrackerEvent.newBuilder()
            .setId("ev-002")
            .setType(TrackerEventType.ACKNOWLEDGE)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(2000).build())
            .build()

        assertEquals(TrackerStatus.SUBMITTED, EventBuilder.latestStatus(listOf(ev1, ev2)))
    }

    @Test
    fun testLatestStatusUsesSequenceNumberOrder() {
        val ev1 = TrackerEvent.newBuilder()
            .setId("ev-001")
            .setType(TrackerEventType.APPLY)
            .setStatusTo(TrackerStatus.SUBMITTED)
            .setSequenceNumber(1)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(2000).build())
            .build()

        val ev2 = TrackerEvent.newBuilder()
            .setId("ev-002")
            .setType(TrackerEventType.SCHEDULE_INTERVIEW)
            .setStatusTo(TrackerStatus.INTERVIEWING)
            .setSequenceNumber(2)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1000).build())
            .build()

        // Sequence number takes precedence over timestamp
        assertEquals(TrackerStatus.INTERVIEWING, EventBuilder.latestStatus(listOf(ev1, ev2)))
    }

    // === Actor Normalization ===

    @Test
    fun testNormalizeActorFromString() {
        assertEquals(EventActor.ACTOR_USER, EventBuilder.normalizeActor("user"))
        assertEquals(EventActor.ACTOR_USER, EventBuilder.normalizeActor("USER"))
        assertEquals(EventActor.ACTOR_USER, EventBuilder.normalizeActor("actor:user"))
        assertEquals(EventActor.ACTOR_AGENT, EventBuilder.normalizeActor("agent"))
        assertEquals(EventActor.ACTOR_EMAIL_SYNC, EventBuilder.normalizeActor("email_sync"))
        assertEquals(EventActor.ACTOR_IMPORT, EventBuilder.normalizeActor("import"))
        assertEquals(EventActor.ACTOR_SCHEDULER, EventBuilder.normalizeActor("scheduler"))
    }

    @Test
    fun testNormalizeActorRejectsUnknown() {
        try {
            EventBuilder.normalizeActor("unknown_actor")
            assertTrue(false, "should have thrown EventError")
        } catch (e: EventError) {
            // expected
        }
    }

    // === DerivedFrom Normalization ===

    @Test
    fun testNormalizeDerivedFromString() {
        assertEquals(DerivedFrom.DERIVED_MANUAL, EventBuilder.normalizeDerivedFrom("manual"))
        assertEquals(DerivedFrom.DERIVED_MANUAL, EventBuilder.normalizeDerivedFrom("MANUAL"))
        assertEquals(DerivedFrom.DERIVED_MANUAL, EventBuilder.normalizeDerivedFrom("derivedFrom:manual"))
        assertEquals(DerivedFrom.DERIVED_NOTES_REGEX, EventBuilder.normalizeDerivedFrom("notes_regex"))
        assertEquals(DerivedFrom.DERIVED_NOTES_REGEX, EventBuilder.normalizeDerivedFrom("notes-regex"))
        assertEquals(DerivedFrom.DERIVED_EMAIL, EventBuilder.normalizeDerivedFrom("email"))
        assertEquals(DerivedFrom.DERIVED_CSV_IMPORT, EventBuilder.normalizeDerivedFrom("csv_import"))
        assertEquals(DerivedFrom.DERIVED_CSV_IMPORT, EventBuilder.normalizeDerivedFrom("csv-import"))
    }

    @Test
    fun testNormalizeDerivedFromRejectsUnknown() {
        try {
            EventBuilder.normalizeDerivedFrom("unknown_source")
            assertTrue(false, "should have thrown EventError")
        } catch (e: EventError) {
            // expected
        }
    }

    // === Confidence Normalization ===

    @Test
    fun testNormalizeConfidenceFromString() {
        assertEquals(Confidence.HIGH_CONFIDENCE, EventBuilder.normalizeConfidence("high"))
        assertEquals(Confidence.HIGH_CONFIDENCE, EventBuilder.normalizeConfidence("HIGH_CONFIDENCE"))
        assertEquals(Confidence.HIGH_CONFIDENCE, EventBuilder.normalizeConfidence("confidence:high"))
        assertEquals(Confidence.MEDIUM_CONFIDENCE, EventBuilder.normalizeConfidence("medium"))
        assertEquals(Confidence.LOW_CONFIDENCE, EventBuilder.normalizeConfidence("low"))
    }

    @Test
    fun testNormalizeConfidenceRejectsUnknown() {
        try {
            EventBuilder.normalizeConfidence("unknown_level")
            assertTrue(false, "should have thrown EventError")
        } catch (e: EventError) {
            // expected
        }
    }

    // === Build Event ===

    @Test
    fun testBuildEventSetsDefaults() {
        val event = EventBuilder.buildEvent(
            eventType = TrackerEventType.APPLY,
        )

        assertEquals(TrackerEventType.APPLY, event.type)
        assertEquals(TrackerStatus.SUBMITTED, event.statusTo)
        assertEquals(EventActor.ACTOR_USER, event.actor)
        assertEquals(DerivedFrom.DERIVED_MANUAL, event.derivedFrom)
        assertEquals(Confidence.HIGH_CONFIDENCE, event.confidence)
        assertTrue(event.id.isNotEmpty())
        assertTrue(event.hasCreatedAt())
    }

    @Test
    fun testBuildEventWithCustomValues() {
        val event = EventBuilder.buildEvent(
            eventType = TrackerEventType.RECRUITER_SCREEN_EVENT,
            actor = EventActor.ACTOR_EMAIL_SYNC,
            derivedFrom = DerivedFrom.DERIVED_EMAIL,
            confidence = Confidence.MEDIUM_CONFIDENCE,
            statusFrom = TrackerStatus.SUBMITTED,
            note = "Recruiter called back",
            emailMessageId = "<msg-123@example.com>",
        )

        assertEquals(TrackerEventType.RECRUITER_SCREEN_EVENT, event.type)
        assertEquals(TrackerStatus.RECRUITER_SCREENED, event.statusTo)
        assertEquals(TrackerStatus.SUBMITTED, event.statusFrom)
        assertEquals(EventActor.ACTOR_EMAIL_SYNC, event.actor)
        assertEquals(DerivedFrom.DERIVED_EMAIL, event.derivedFrom)
        assertEquals(Confidence.MEDIUM_CONFIDENCE, event.confidence)
        assertEquals("Recruiter called back", event.note)
        assertEquals("<msg-123@example.com>", event.emailMessageId)
    }

    @Test
    fun testBuildEventNonStatusEventHasNoStatusTo() {
        val event = EventBuilder.buildEvent(
            eventType = TrackerEventType.ACKNOWLEDGE,
        )

        assertEquals(TrackerEventType.ACKNOWLEDGE, event.type)
        assertEquals(TrackerStatus.TRACKER_STATUS_UNSPECIFIED, event.statusTo)
    }

    @Test
    fun testBuildEventWithExplicitStatusTo() {
        val event = EventBuilder.buildEvent(
            eventType = TrackerEventType.APPLY,
            statusTo = TrackerStatus.INTERVIEWING,
        )

        assertEquals(TrackerStatus.INTERVIEWING, event.statusTo)
    }

    @Test
    fun testBuildEventWithInterviewRound() {
        val round = InterviewRound.newBuilder()
            .setCurrentRound(1)
            .setTotalRounds(3)
            .setType(InterviewType.TECHNICAL_KNOWLEDGE)
            .setTitle("Technical Deep Dive")
            .build()

        val event = JobApplicationEventBuilder.buildEvent(
            eventType = TrackerEventType.SCHEDULE_INTERVIEW,
            interviewRound = round,
        )

        assertEquals(TrackerStatus.INTERVIEWING, event.statusTo)
        assertTrue(event.hasInterviewRound())
        assertEquals(1, event.interviewRound.currentRound)
        assertEquals(3, event.interviewRound.totalRounds)
        assertEquals(InterviewType.TECHNICAL_KNOWLEDGE, event.interviewRound.type)
    }

    // === Normalize Date ===

    @Test
    fun testNormalizeDateBareDate() {
        val ts = EventBuilder.normalizeDate("2024-01-15")
        // 2024-01-15T00:00:00Z = 1705276800 epoch seconds
        assertEquals(1705276800, ts.seconds)
        assertEquals(0, ts.nanos)
    }

    @Test
    fun testNormalizeDateWithTimestamp() {
        val ts = EventBuilder.normalizeDate("2024-01-15T10:30:00")
        assertTrue(ts.seconds > 0)
    }

    @Test
    fun testNormalizeDateWithZuluSuffix() {
        val ts = EventBuilder.normalizeDate("2024-01-15T10:30:00Z")
        assertTrue(ts.seconds > 0)
    }

    @Test
    fun testNormalizeDateNullReturnsNow() {
        val ts = EventBuilder.normalizeDate(null)
        assertTrue(ts.seconds > 0)
        val now = java.time.Instant.now().epochSecond
        assertTrue(kotlin.math.abs(ts.seconds - now) < 5)
    }

    @Test
    fun testNormalizeDateRejectsInvalid() {
        try {
            EventBuilder.normalizeDate("not-a-date")
            assertTrue(false, "should have thrown EventError")
        } catch (e: EventError) {
            // expected
        }
    }

    @Test
    fun testNormalizeDateRejectsEmpty() {
        try {
            EventBuilder.normalizeDate("")
            assertTrue(false, "should have thrown EventError")
        } catch (e: EventError) {
            // expected
        }
    }

    // === Status Event Type ===

    @Test
    fun testStatusEventTypeReturnsCanonicalEvent() {
        val eventType = EventBuilder.statusEventType(TrackerStatus.SUBMITTED)
        assertEquals(TrackerEventType.APPLY, eventType)
    }

    @Test
    fun testStatusEventTypeForDraftingReturnsCorrectStatus() {
        val eventType = EventBuilder.statusEventType(TrackerStatus.DRAFTED)
        assertEquals(TrackerEventType.CORRECT_STATUS, eventType)
    }

    @Test
    fun testStatusEventTypeForHired() {
        val eventType = EventBuilder.statusEventType(TrackerStatus.HIRED)
        assertEquals(TrackerEventType.ACCEPT_OFFER, eventType)
    }
}
