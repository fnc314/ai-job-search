package dev.fnc314.tools.jobsearch.parser

import dev.fnc314.tools.jobsearch.tracker.v1.SubmissionConfidence
import dev.fnc314.tools.jobsearch.tracker.v1.SubmissionConfirmation
import dev.fnc314.tools.jobsearch.tracker.v1.Timeline
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import com.google.protobuf.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Pure projection engine computing derived timelines, official submission timestamps,
 * submission confirmations, and counters from strongly typed protobuf events.
 */
public object ApplicationTimelineProjector {

    private val APPLY_EVENT_TYPES: Set<TrackerEventType> = setOf(
        TrackerEventType.APPLY,
    )

    private val ACK_EVENT_TYPES: Set<TrackerEventType> = setOf(
        TrackerEventType.ACKNOWLEDGE,
    )

    private val INTERVIEW_EVENT_TYPES: Set<TrackerEventType> = setOf(
        TrackerEventType.SCHEDULE_INTERVIEW,
        TrackerEventType.VISIT_ONSITE,
        TrackerEventType.RECRUITER_SCREEN_EVENT,
    )

    private val REJECTION_EVENT_TYPES: Set<TrackerEventType> = setOf(
        TrackerEventType.REJECT,
    )

    private fun protoTimestampToInstant(ts: Timestamp): Instant? {
        if (ts.seconds == 0L && ts.nanos == 0) return null
        return Instant.ofEpochSecond(ts.seconds, ts.nanos.toLong())
    }

    private fun instantToProtoTimestamp(instant: Instant): Timestamp {
        return Timestamp.newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()
    }

    /**
     * Derives [Timeline] containing first/last activity, submittedAt, submissionConfirmedAt,
     * and days since last activity.
     */
    public fun projectTimeline(
        events: List<TrackerEvent>,
        evaluationInstant: Instant = Instant.now(),
    ): Timeline {
        val instants = events.mapNotNull { protoTimestampToInstant(it.createdAt) }
        val builder = Timeline.newBuilder()

        if (instants.isEmpty()) {
            return builder.build()
        }

        val first = instants.minOrNull()!!
        val last = instants.maxOrNull()!!
        val daysSince = ChronoUnit.DAYS.between(last, evaluationInstant).coerceAtLeast(0)

        builder.setFirstActivityAt(instantToProtoTimestamp(first))
            .setLastActivityAt(instantToProtoTimestamp(last))
            .setDaysSinceActivity(daysSince.toInt())

        // Derive submittedAt from earliest APPLY event
        val applyInstants = events.filter { it.type in APPLY_EVENT_TYPES }
            .mapNotNull { protoTimestampToInstant(it.createdAt) }
        if (applyInstants.isNotEmpty()) {
            builder.setSubmittedAt(instantToProtoTimestamp(applyInstants.minOrNull()!!))
        }

        // Derive submissionConfirmedAt from earliest ACKNOWLEDGE event
        val ackInstants = events.filter { it.type in ACK_EVENT_TYPES }
            .mapNotNull { protoTimestampToInstant(it.createdAt) }
        if (ackInstants.isNotEmpty()) {
            builder.setSubmissionConfirmedAt(instantToProtoTimestamp(ackInstants.minOrNull()!!))
        }

        return builder.build()
    }

    /**
     * Derives [SubmissionConfirmation] based on existing confirmation state and event logs.
     */
    public fun projectSubmissionConfirmation(
        existing: SubmissionConfirmation?,
        events: List<TrackerEvent>,
    ): SubmissionConfirmation {
        val ackEvents = events.filter { it.type in ACK_EVENT_TYPES }
            .sortedBy { it.createdAt.seconds }
        val applyEvents = events.filter { it.type in APPLY_EVENT_TYPES }
            .sortedBy { it.createdAt.seconds }

        if (ackEvents.isNotEmpty()) {
            val earliestAck = ackEvents.first()
            val applyInstants = applyEvents.mapNotNull { protoTimestampToInstant(it.createdAt) }
            val earliestApplyTs = if (applyInstants.isNotEmpty()) {
                instantToProtoTimestamp(applyInstants.minOrNull()!!)
            } else {
                earliestAck.createdAt
            }

            return SubmissionConfirmation.newBuilder()
                .setIsConfirmed(true)
                .setConfidence(SubmissionConfidence.CONFIRMED_SCRAPED)
                .setSubmittedAt(earliestApplyTs)
                .setConfirmedAt(earliestAck.createdAt)
                .setConfirmationEmailMessageId(earliestAck.emailMessageId)
                .setSourceSubject(earliestAck.note.ifBlank { "Employer confirmation email scraped and matched" })
                .build()
        }

        if (existing != null && existing.isConfirmed) {
            return existing
        }

        if (applyEvents.isNotEmpty()) {
            val earliestApply = applyEvents.first()
            return SubmissionConfirmation.newBuilder()
                .setIsConfirmed(false)
                .setConfidence(SubmissionConfidence.UNCONFIRMED_USER_MARKED)
                .setSubmittedAt(earliestApply.createdAt)
                .setNotes("User marked application as submitted without employer acknowledgment email")
                .build()
        }

        return SubmissionConfirmation.newBuilder()
            .setIsConfirmed(false)
            .setConfidence(SubmissionConfidence.SUBMISSION_CONFIDENCE_UNSPECIFIED)
            .build()
    }

    /**
     * Computes metric counters from the event log.
     */
    public fun projectCounters(events: List<TrackerEvent>): Map<String, Int> {
        var interviews = 0
        var rejections = 0
        var followUpsSent = 0

        for (event in events) {
            when (event.type) {
                in INTERVIEW_EVENT_TYPES -> interviews++
                in REJECTION_EVENT_TYPES -> rejections++
                TrackerEventType.SEND_FOLLOW_UP -> followUpsSent++
                else -> Unit
            }
        }

        return mapOf(
            "events" to events.size,
            "interviews" to interviews,
            "rejections" to rejections,
            "followUpsSent" to followUpsSent,
        )
    }

    /**
     * Resolves human-readable history formatted with timestamps and notes via [EventNoteRenderer].
     */
    public fun projectNotes(events: List<TrackerEvent>): String? {
        return EventNoteRenderer.renderNotes(events)
    }

    /**
     * Finds the latest implied funnel status from status-bearing events.
     */
    public fun latestStatus(events: List<TrackerEvent>): TrackerStatus? {
        val sorted = events.sortedWith(
            compareBy<TrackerEvent> { if (it.sequenceNumber > 0) it.sequenceNumber else Long.MAX_VALUE }
                .thenBy { it.createdAt.seconds }
                .thenBy { it.id }
        )

        return sorted.lastOrNull { it.statusTo != TrackerStatus.TRACKER_STATUS_UNSPECIFIED }?.statusTo
    }
}
