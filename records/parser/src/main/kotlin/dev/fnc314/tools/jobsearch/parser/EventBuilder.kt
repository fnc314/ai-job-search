package dev.fnc314.tools.jobsearch.parser

import com.google.protobuf.Timestamp
import dev.fnc314.tools.jobsearch.tracker.v1.Confidence
import dev.fnc314.tools.jobsearch.tracker.v1.DerivedFrom
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Thrown when an event cannot be recorded as requested.
 */
public class EventError(message: String) : IllegalArgumentException(message)

/**
 * Pure domain logic for constructing and validating [TrackerEvent] instances.
 *
 * Mirrors the Python `events.py` module: event building with actor/derivedFrom/confidence
 * normalization, implied status derivation, latest status calculation, and date parsing.
 *
 * Two rules live here:
 *   1. An event is built and validated as a whole, so a caller cannot write a
 *      half-formed history entry.
 *   2. Provenance is mandatory. Migrated or inferred history says so, via
 *      `derivedFrom` and `confidence`, and never presents as recorded fact.
 */
public object EventBuilder {

    // === Implied Status Map ===

    /**
     * Event types that imply a funnel status transition.
     * An event carries statusTo only when it actually moves the funnel.
     */
    private val eventImpliedStatus: Map<TrackerEventType, TrackerStatus> = mapOf(
        TrackerEventType.APPLY to TrackerStatus.APPLIED,
        TrackerEventType.RECRUITER_SCREEN to TrackerStatus.INTERVIEW,
        TrackerEventType.SCHEDULE_INTERVIEW to TrackerStatus.INTERVIEW,
        TrackerEventType.VISIT_ONSITE to TrackerStatus.INTERVIEW,
        TrackerEventType.OFFER to TrackerStatus.OFFER_EXPECTED,
        TrackerEventType.ACCEPT_OFFER to TrackerStatus.HIRED,
        TrackerEventType.DECLINE_OFFER to TrackerStatus.OFFER_DECLINED,
        TrackerEventType.REJECT to TrackerStatus.REJECTED,
        TrackerEventType.GHOST to TrackerStatus.NO_RESPONSE,
        TrackerEventType.WITHDRAW to TrackerStatus.APPLICATION_WITHDRAWN,
    )

    /**
     * Returns the status an event type implies, or null if it does not move the funnel.
     */
    public fun impliedStatus(eventType: TrackerEventType): TrackerStatus? {
        return eventImpliedStatus[eventType]
    }

    /**
     * Returns the canonical event type for entering a given status.
     * DRAFTING has no inbound event type, so returns CORRECT_STATUS.
     */
    public fun statusEventType(status: TrackerStatus): TrackerEventType {
        for ((eventType, implied) in eventImpliedStatus) {
            if (implied == status) return eventType
        }
        return TrackerEventType.CORRECT_STATUS
    }

    // === Latest Status ===

    /**
     * Returns the status implied by the last status-bearing event, if any.
     * Events are sorted by sequence number first, then by timestamp.
     */
    public fun latestStatus(events: List<TrackerEvent>): TrackerStatus? {
        val sorted = events.sortedWith(
            compareBy<TrackerEvent> { if (it.sequenceNumber > 0) it.sequenceNumber else Long.MAX_VALUE }
                .thenBy { it.createdAt.seconds }
                .thenBy { it.id }
        )
        for (i in sorted.size - 1 downTo 0) {
            val event = sorted[i]
            if (event.statusTo != TrackerStatus.TRACKER_STATUS_UNSPECIFIED) {
                return event.statusTo
            }
        }
        return null
    }

    // === Actor Normalization ===

    private val actorMap: Map<String, EventActor> = mapOf(
        "user" to EventActor.ACTOR_USER,
        "agent" to EventActor.ACTOR_AGENT,
        "email_sync" to EventActor.ACTOR_EMAIL_SYNC,
        "import" to EventActor.ACTOR_IMPORT,
        "scheduler" to EventActor.ACTOR_SCHEDULER,
    )

    /**
     * Normalizes a raw actor string to an [EventActor] enum value.
     * Handles "actor:user" prefix and case-insensitive matching.
     */
    public fun normalizeActor(raw: String): EventActor {
        var text = raw.trim()
        if (text.startsWith("actor:")) {
            text = text.substringAfter(":", "").lowercase()
        } else {
            text = text.lowercase()
        }
        return actorMap[text] ?: throw EventError("unknown actor: $raw")
    }

    // === DerivedFrom Normalization ===

    private val derivedFromMap: Map<String, DerivedFrom> = mapOf(
        "manual" to DerivedFrom.DERIVED_MANUAL,
        "notes_regex" to DerivedFrom.DERIVED_NOTES_REGEX,
        "notes-regex" to DerivedFrom.DERIVED_NOTES_REGEX,
        "email" to DerivedFrom.DERIVED_EMAIL,
        "csv_import" to DerivedFrom.DERIVED_CSV_IMPORT,
        "csv-import" to DerivedFrom.DERIVED_CSV_IMPORT,
    )

    /**
     * Normalizes a raw derivedFrom string to a [DerivedFrom] enum value.
     * Handles "derivedFrom:" prefix and aliases.
     */
    public fun normalizeDerivedFrom(raw: String): DerivedFrom {
        var text = raw.trim()
        if (text.startsWith("derived-from:") || text.startsWith("derivedFrom:")) {
            text = text.substringAfter(":", "").lowercase()
        } else {
            text = text.lowercase()
        }
        return derivedFromMap[text] ?: throw EventError("unknown derivedFrom: $raw")
    }

    // === Confidence Normalization ===

    private val confidenceMap: Map<String, Confidence> = mapOf(
        "high" to Confidence.HIGH_CONFIDENCE,
        "medium" to Confidence.MEDIUM_CONFIDENCE,
        "low" to Confidence.LOW_CONFIDENCE,
    )

    /**
     * Normalizes a raw confidence string to a [Confidence] enum value.
     * Handles "confidence:" prefix and exact enum name matching.
     */
    public fun normalizeConfidence(raw: String): Confidence {
        var text = raw.trim()
        if (text.startsWith("confidence:")) {
            text = text.substringAfter(":", "").lowercase()
        } else {
            // Handle exact enum names like "HIGH_CONFIDENCE"
            val lower = text.lowercase()
            if (lower == "high_confidence") return Confidence.HIGH_CONFIDENCE
            if (lower == "medium_confidence") return Confidence.MEDIUM_CONFIDENCE
            if (lower == "low_confidence") return Confidence.LOW_CONFIDENCE
            text = lower
        }
        return confidenceMap[text] ?: throw EventError("unknown confidence: $raw")
    }

    // === Date Normalization ===

    /**
     * Parses a bare date or full timestamp into a protobuf [Timestamp].
     * Accepts "YYYY-MM-DD", "YYYY-MM-DDTHH:MM:SS", and "YYYY-MM-DDTHH:MM:SSZ".
     * Returns the current time if null is passed.
     */
    public fun normalizeDate(value: String?): Timestamp {
        if (value == null) {
            return nowTimestamp()
        }
        val text = value.trim()
        if (text.isEmpty()) {
            throw EventError("date is empty")
        }
        return try {
            if ('T' !in text) {
                // Bare date: treat as calendar fact, convert to UTC midnight
                val localDate = LocalDate.parse(text)
                val epochSeconds = localDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
                Timestamp.newBuilder().setSeconds(epochSeconds).setNanos(0).build()
            } else {
                // Full timestamp
                val normalized = text.replace("Z", "+00:00")
                val dateTime = LocalDateTime.parse(normalized.substringBefore('+'))
                val epochSeconds = dateTime.atZone(ZoneOffset.UTC).toEpochSecond()
                Timestamp.newBuilder().setSeconds(epochSeconds).setNanos(0).build()
            }
        } catch (e: Exception) {
            throw EventError("date is not ISO 8601: $value")
        }
    }

    /**
     * Returns the current time as a protobuf [Timestamp].
     */
    public fun nowTimestamp(): Timestamp {
        val now = Instant.now()
        return Timestamp.newBuilder()
            .setSeconds(now.epochSecond)
            .setNanos(now.nano)
            .build()
    }

    // === Build Event ===

    /**
     * Constructs and validates one [TrackerEvent].
     *
     * `statusTo` defaults to the status implied by the event type.
     * `actor`, `derivedFrom`, and `confidence` default to user/manual/high.
     *
     * @param eventType The type of event (required).
     * @param date Optional ISO date string; defaults to now.
     * @param note Optional free-text note.
     * @param actor Who created this event; defaults to USER.
     * @param derivedFrom Provenance source; defaults to MANUAL.
     * @param confidence Confidence level; defaults to HIGH_CONFIDENCE.
     * @param statusFrom Optional prior status; callers should pass known prior state.
     * @param statusTo Optional target status; defaults to implied status.
     * @param emailMessageId Optional email message ID reference.
     * @return A fully constructed [TrackerEvent] with UUIDv7 ID and timestamp.
     */
    public fun buildEvent(
        eventType: TrackerEventType,
        date: String? = null,
        note: String? = null,
        actor: EventActor = EventActor.ACTOR_USER,
        derivedFrom: DerivedFrom = DerivedFrom.DERIVED_MANUAL,
        confidence: Confidence = Confidence.HIGH_CONFIDENCE,
        statusFrom: TrackerStatus? = null,
        statusTo: TrackerStatus? = null,
        emailMessageId: String? = null,
    ): TrackerEvent {
        val resolvedDate = normalizeDate(date)
        val resolvedStatusTo = statusTo ?: impliedStatus(eventType) ?: TrackerStatus.TRACKER_STATUS_UNSPECIFIED

        val builder = TrackerEvent.newBuilder()
            .setId(UuidGenerator.newUuid())
            .setType(eventType)
            .setActor(actor)
            .setDerivedFrom(derivedFrom)
            .setConfidence(confidence)
            .setStatusTo(resolvedStatusTo)
            .setCreatedAt(resolvedDate)

        if (statusFrom != null) {
            builder.setStatusFrom(statusFrom)
        }
        if (!note.isNullOrBlank()) {
            builder.setNote(note)
        }
        if (!emailMessageId.isNullOrBlank()) {
            builder.setEmailMessageId(emailMessageId)
        }

        return builder.build()
    }
}
