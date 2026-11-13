package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobsearch.parser.ApplicationTimelineProjector
import dev.fnc314.tools.jobsearch.parser.EventChainBuilder
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider
import java.time.Instant

public class AddEventCommand : CliktCommand(name = "event") {
    private val applicationId: String by argument("id", help = "Application slug ID")
    private val eventType: String by argument("event_type", help = "Event type name (e.g. SEND_FOLLOW_UP, INTERVIEW)")
    private val note: String? by option("--note", help = "Optional context note")
    private val emailMessageId: String? by option("--email-message-id", help = "Associated email message ID")

    override fun run() {
        val t = Terminal()
        FirestoreProvider.assertWritesAllowed("timeline event append")

        val db = FirestoreProvider.firestore
        val docRef = db.collection("applications").document(applicationId)
        val snap = docRef.get().get()

        if (!snap.exists()) {
            t.println(red("Application not found: $applicationId"))
            return
        }

        val eventsColl = docRef.collection("events")
        val allEventsDocs = eventsColl.get().get().documents
        val existingEvents = mutableListOf<TrackerEvent>()

        for (d in allEventsDocs) {
            val data = d.data
            val b = TrackerEvent.newBuilder().setId(d.id)
            val typeStr = (data["type"] as? String) ?: (data["eventType"] as? String) ?: "ADD_NOTE"
            try {
                b.setType(TrackerEventType.valueOf(typeStr.uppercase()))
            } catch (e: Exception) {
                b.setType(TrackerEventType.ADD_NOTE)
            }
            val actorStr = (data["actor"] as? String) ?: "ACTOR_USER"
            try {
                b.setActor(EventActor.valueOf(actorStr.uppercase()))
            } catch (e: Exception) {
                b.setActor(EventActor.ACTOR_USER)
            }
            (data["sequenceNumber"] as? Number)?.let { b.setSequenceNumber(it.toLong()) }
            (data["eventHash"] as? String)?.let { b.setEventHash(it) }
            (data["prevEventHash"] as? String)?.let { b.setPrevEventHash(it) }
            // Handle ts/date as either String or Firestore Timestamp
            val tsStr = when (val ts = data["ts"]) {
                is String -> ts.takeIf { it.isNotBlank() }
                is com.google.cloud.Timestamp -> Instant.ofEpochSecond(ts.seconds, ts.nanos.toLong()).toString()
                else -> null
            } ?: when (val d = data["date"]) {
                is String -> d.takeIf { it.isNotBlank() }
                is com.google.cloud.Timestamp -> Instant.ofEpochSecond(d.seconds, d.nanos.toLong()).toString()
                else -> null
            }
            if (!tsStr.isNullOrBlank()) {
                try {
                    val inst = Instant.parse(tsStr)
                    b.setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(inst.epochSecond).setNanos(inst.nano).build())
                } catch (ignored: Exception) {}
            }
            existingEvents.add(b.build())
        }

        val targetEventId = java.util.UUID.randomUUID().toString()
        val nowInstant = Instant.now()
        val incomingBuilder = TrackerEvent.newBuilder()
            .setId(targetEventId)
            .setApplicationId(applicationId)
            .setActor(EventActor.ACTOR_USER)
            .setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(nowInstant.epochSecond).setNanos(nowInstant.nano).build())

        try {
            incomingBuilder.setType(TrackerEventType.valueOf(eventType.uppercase()))
        } catch (e: Exception) {
            incomingBuilder.setType(TrackerEventType.ADD_NOTE)
        }
        note?.let { incomingBuilder.setNote(it) }
        emailMessageId?.let { incomingBuilder.setEmailMessageId(it) }

        val chained = EventChainBuilder.computeNextLink(
            existingEvents = existingEvents,
            newEvent = incomingBuilder.build(),
            applicationId = applicationId
        )

        val eventData = mutableMapOf<String, Any?>(
            "eventId" to chained.id,
            "ts" to nowInstant.toString(),
            "date" to nowInstant.toString().substringBefore("T"),
            "type" to chained.type.name,
            "actor" to chained.actor.name,
            "sequenceNumber" to chained.sequenceNumber,
            "prevEventHash" to chained.prevEventHash,
            "eventHash" to chained.eventHash
        )
        if (note != null) {
            eventData["note"] = note!!
        }
        if (emailMessageId != null) {
            eventData["emailMessageId"] = emailMessageId!!
        }

        eventsColl.document(chained.id).set(eventData).get()

        // Reproject parent timeline and counters
        val updatedEvents = existingEvents + chained
        val timeline = ApplicationTimelineProjector.projectTimeline(updatedEvents)
        val confirmation = ApplicationTimelineProjector.projectSubmissionConfirmation(null, updatedEvents)
        val counters = ApplicationTimelineProjector.projectCounters(updatedEvents)

        val parentUpdate = mutableMapOf<String, Any?>(
            "updatedAt" to Timestamp.now(),
            "timeline" to mapOf(
                "submittedAt" to if (timeline.hasSubmittedAt() && timeline.submittedAt.seconds > 0)
                    Instant.ofEpochSecond(timeline.submittedAt.seconds).toString() else null,
                "submissionConfirmedAt" to if (timeline.hasSubmissionConfirmedAt() && timeline.submissionConfirmedAt.seconds > 0)
                    Instant.ofEpochSecond(timeline.submissionConfirmedAt.seconds).toString() else null,
                "firstActivityAt" to if (timeline.hasFirstActivityAt() && timeline.firstActivityAt.seconds > 0)
                    Instant.ofEpochSecond(timeline.firstActivityAt.seconds).toString() else null,
                "lastActivityAt" to if (timeline.hasLastActivityAt() && timeline.lastActivityAt.seconds > 0)
                    Instant.ofEpochSecond(timeline.lastActivityAt.seconds).toString() else null,
                "daysSinceActivity" to timeline.daysSinceActivity
            ),
            "submissionConfirmation" to mapOf(
                "isConfirmed" to confirmation.isConfirmed,
                "confidence" to confirmation.confidence.name,
                "submittedAt" to if (confirmation.hasSubmittedAt() && confirmation.submittedAt.seconds > 0)
                    Instant.ofEpochSecond(confirmation.submittedAt.seconds).toString() else null,
                "confirmedAt" to if (confirmation.hasConfirmedAt() && confirmation.confirmedAt.seconds > 0)
                    Instant.ofEpochSecond(confirmation.confirmedAt.seconds).toString() else null,
                "confirmationEmailMessageId" to confirmation.confirmationEmailMessageId.takeIf { it.isNotBlank() },
                "sourceSubject" to confirmation.sourceSubject.takeIf { it.isNotBlank() }
            ),
            "counters" to counters
        )

        docRef.set(parentUpdate, com.google.cloud.firestore.SetOptions.merge()).get()

        t.println(green("✓ Successfully appended cryptographic event ${chained.type.name} (#${chained.sequenceNumber}) to $applicationId${if (emailMessageId != null) " (linked email: $emailMessageId)" else ""}"))
    }
}
