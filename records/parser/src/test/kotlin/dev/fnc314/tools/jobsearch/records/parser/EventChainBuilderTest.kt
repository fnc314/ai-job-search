package dev.fnc314.tools.jobsearch.records.parser

import com.google.protobuf.Timestamp
import dev.fnc314.tools.jobsearch.parser.EventChainBuilder
import dev.fnc314.tools.jobsearch.parser.EventChainVerifier
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventChainBuilderTest {

    @Test
    fun testEmptyChainVerifies() {
        assertTrue(EventChainVerifier.verifyChain(emptyList()))
    }

    @Test
    fun testComputeNextLinkGenesis() {
        val event = TrackerEvent.newBuilder()
            .setId("ev-001")
            .setApplicationId("app-123")
            .setType(TrackerEventType.APPLY)
            .setActor(EventActor.ACTOR_USER)
            .setStatusTo(TrackerStatus.SUBMITTED)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776000).build())
            .build()

        val chained = EventChainBuilder.computeNextLink(emptyList(), event, "app-123")
        assertEquals(1L, chained.sequenceNumber)
        assertEquals(EventChainBuilder.GENESIS_PREV_HASH, chained.prevEventHash)
        assertTrue(chained.eventHash.isNotBlank())
        assertTrue(EventChainVerifier.verifyChain(listOf(chained)))
    }

    @Test
    fun testChainEventsSequential() {
        val ev1 = TrackerEvent.newBuilder()
            .setId("ev-001")
            .setType(TrackerEventType.APPLY)
            .setActor(EventActor.ACTOR_USER)
            .setStatusTo(TrackerStatus.SUBMITTED)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776000).build())
            .build()

        val ev2 = TrackerEvent.newBuilder()
            .setId("ev-002")
            .setType(TrackerEventType.ACKNOWLEDGE)
            .setActor(EventActor.ACTOR_EMAIL_SYNC)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776300).build())
            .build()

        val chained = EventChainBuilder.chainEvents(listOf(ev2, ev1), "app-123")
        assertEquals(2, chained.size)
        assertEquals(1L, chained[0].sequenceNumber)
        assertEquals("ev-001", chained[0].id)
        assertEquals(EventChainBuilder.GENESIS_PREV_HASH, chained[0].prevEventHash)

        assertEquals(2L, chained[1].sequenceNumber)
        assertEquals("ev-002", chained[1].id)
        assertEquals(chained[0].eventHash, chained[1].prevEventHash)

        assertTrue(EventChainVerifier.verifyChain(chained))
    }

    @Test
    fun testCorruptedChainFailsVerification() {
        val ev1 = TrackerEvent.newBuilder()
            .setId("ev-001")
            .setType(TrackerEventType.APPLY)
            .setActor(EventActor.ACTOR_USER)
            .setStatusTo(TrackerStatus.SUBMITTED)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776000).build())
            .build()

        val ev2 = TrackerEvent.newBuilder()
            .setId("ev-002")
            .setType(TrackerEventType.ACKNOWLEDGE)
            .setActor(EventActor.ACTOR_EMAIL_SYNC)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(1762776300).build())
            .build()

        val chained = EventChainBuilder.chainEvents(listOf(ev1, ev2), "app-123")
        val tampered = listOf(
            chained[0],
            chained[1].toBuilder().setEventHash("0000000000000000000000000000000000000000000000000000000000000000").build()
        )
        assertFalse(EventChainVerifier.verifyChain(tampered))
    }
}
