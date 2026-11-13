package dev.fnc314.tools.jobtracker.eventchain

import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import java.time.Instant

/**
 * Pure domain logic for constructing, chaining, and verifying cryptographic
 * event hash chains (blockchain pattern) for job applications.
 */
public object EventChainBuilder {

    public const val GENESIS_PREV_HASH: String = EventChainVerifier.GENESIS_PREV_HASH

    /**
     * Resolves the timestamp ISO string of an event for hash computation.
     */
    public fun resolveTimestampIso(event: TrackerEvent): String {
        return if (event.hasCreatedAt()) {
            Instant.ofEpochSecond(
                event.createdAt.seconds,
                event.createdAt.nanos.toLong(),
            ).toString()
        } else {
            ""
        }
    }

    /**
     * Given an existing sequence of events (which may be empty) and a new unchained event,
     * computes the next [TrackerEvent] with sequential [TrackerEvent.getSequenceNumber],
     * [TrackerEvent.getPrevEventHash], and SHA-256 [TrackerEvent.getEventHash].
     */
    public fun computeNextLink(
        existingEvents: List<TrackerEvent>,
        newEvent: TrackerEvent,
        applicationId: String,
    ): TrackerEvent {
        val sorted = existingEvents.sortedWith(
            compareBy<TrackerEvent> { if (it.sequenceNumber > 0) it.sequenceNumber else Long.MAX_VALUE }
                .thenBy { it.createdAt.seconds }
                .thenBy { it.id }
        )

        val nextSeq = (sorted.lastOrNull()?.sequenceNumber ?: 0L) + 1L
        val prevHash = sorted.lastOrNull()?.eventHash?.takeIf { it.isNotBlank() } ?: GENESIS_PREV_HASH
        val tsIso = resolveTimestampIso(newEvent)
        val statusToStr = if (newEvent.statusTo != TrackerStatus.TRACKER_STATUS_UNSPECIFIED) {
            newEvent.statusTo.name
        } else {
            null
        }

        val computedHash = EventChainVerifier.computeHash(
            sequenceNumber = nextSeq,
            prevEventHash = prevHash,
            eventId = newEvent.id,
            applicationId = applicationId,
            eventType = newEvent.type.name,
            timestampIso = tsIso,
            statusTo = statusToStr,
            actor = newEvent.actor.name,
        )

        return newEvent.toBuilder()
            .setApplicationId(applicationId)
            .setSequenceNumber(nextSeq)
            .setPrevEventHash(prevHash)
            .setEventHash(computedHash)
            .build()
    }

    /**
     * Takes an arbitrary collection of events for an application, sorts them chronologically,
     * and recalculates an unbroken hash chain from genesis to head.
     */
    public fun chainEvents(
        events: List<TrackerEvent>,
        applicationId: String,
    ): List<TrackerEvent> {
        val sorted = events.sortedWith(
            compareBy<TrackerEvent> { if (it.sequenceNumber > 0) it.sequenceNumber else Long.MAX_VALUE }
                .thenBy { it.createdAt.seconds }
                .thenBy { it.id }
        )

        val chained = mutableListOf<TrackerEvent>()
        var expectedPrevHash = GENESIS_PREV_HASH
        var expectedSequence = 1L

        for (event in sorted) {
            val tsIso = resolveTimestampIso(event)
            val statusToStr = if (event.statusTo != TrackerStatus.TRACKER_STATUS_UNSPECIFIED) {
                event.statusTo.name
            } else {
                null
            }

            val computedHash = EventChainVerifier.computeHash(
                sequenceNumber = expectedSequence,
                prevEventHash = expectedPrevHash,
                eventId = event.id,
                applicationId = applicationId,
                eventType = event.type.name,
                timestampIso = tsIso,
                statusTo = statusToStr,
                actor = event.actor.name,
            )

            val chainedEvent = event.toBuilder()
                .setApplicationId(applicationId)
                .setSequenceNumber(expectedSequence)
                .setPrevEventHash(expectedPrevHash)
                .setEventHash(computedHash)
                .build()

            chained.add(chainedEvent)
            expectedPrevHash = computedHash
            expectedSequence++
        }
        return chained
    }
}
