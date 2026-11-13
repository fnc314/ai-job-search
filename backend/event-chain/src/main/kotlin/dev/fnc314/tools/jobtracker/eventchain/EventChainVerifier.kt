package dev.fnc314.tools.jobtracker.eventchain

import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import java.security.MessageDigest

/**
 * Cryptographic event hash chain verifier implementing a blockchain pattern
 * for tamper-evident, strictly ordered, immutable tracker timeline logs.
 */
public object EventChainVerifier {

    public const val GENESIS_PREV_HASH: String =
        "0000000000000000000000000000000000000000000000000000000000000000"

    /**
     * Computes the SHA-256 hash of a [TrackerEvent] given its sequence number, previous hash,
     * core identifiers, event type, timestamp, target status, and actor.
     */
    public fun computeHash(
        sequenceNumber: Long,
        prevEventHash: String,
        eventId: String,
        applicationId: String,
        eventType: String,
        timestampIso: String,
        statusTo: String?,
        actor: String,
    ): String {
        val payload = "$sequenceNumber|$prevEventHash|$eventId|$applicationId|$eventType|$timestampIso|${statusTo ?: ""}|$actor"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies the cryptographic integrity of a sequence of [TrackerEvent] instances.
     * Returns true if all hashes match, sequence numbers are strictly incrementing from 1,
     * and each event points to the exact hash of its predecessor.
     */
    public fun verifyChain(events: List<TrackerEvent>): Boolean {
        if (events.isEmpty()) return true
        var expectedPrevHash = GENESIS_PREV_HASH
        var expectedSequence = 1L

        for (event in events) {
            if (event.sequenceNumber != expectedSequence) return false
            if (event.prevEventHash != expectedPrevHash) return false

            val tsIso = if (event.hasCreatedAt()) {
                java.time.Instant.ofEpochSecond(
                    event.createdAt.seconds,
                    event.createdAt.nanos.toLong(),
                ).toString()
            } else {
                ""
            }

            val computed = computeHash(
                sequenceNumber = event.sequenceNumber,
                prevEventHash = event.prevEventHash,
                eventId = event.id,
                applicationId = event.applicationId,
                eventType = event.type.name,
                timestampIso = tsIso,
                statusTo = if (event.statusTo != TrackerStatus.TRACKER_STATUS_UNSPECIFIED) event.statusTo.name else null,
                actor = event.actor.name,
            )

            if (event.eventHash != computed) return false

            expectedPrevHash = event.eventHash
            expectedSequence++
        }
        return true
    }
}
