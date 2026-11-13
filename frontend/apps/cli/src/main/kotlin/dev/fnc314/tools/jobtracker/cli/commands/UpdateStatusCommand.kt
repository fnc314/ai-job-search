package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobsearch.parser.EventChainBuilder
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider
import java.time.Instant

public class UpdateStatusCommand : CliktCommand(name = "status") {
    private val applicationId: String by argument("id", help = "Application slug ID")
    private val newStatus: String by argument("status", help = "New funnel status value")
    private val note: String? by option("--note", help = "Optional context note")

    override fun run() {
        val t = Terminal()
        FirestoreProvider.assertWritesAllowed("status change")

        val db = FirestoreProvider.firestore
        val docRef = db.collection("applications").document(applicationId)
        val snap = docRef.get().get()

        if (!snap.exists()) {
            t.println(red("Application not found: $applicationId"))
            return
        }

        val previousStatus = snap.getString("status") ?: "UNKNOWN"
        val now = Timestamp.now()
        val nowInstant = Instant.ofEpochSecond(now.seconds, now.nanos.toLong())

        // Load existing events for hash chain
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
            } ?: when (val d2 = data["date"]) {
                is String -> d2.takeIf { it.isNotBlank() }
                is com.google.cloud.Timestamp -> Instant.ofEpochSecond(d2.seconds, d2.nanos.toLong()).toString()
                else -> null
            }
            if (!tsStr.isNullOrBlank()) {
                try {
                    val inst = Instant.parse(tsStr)
                    b.setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(inst.epochSecond).setNanos(inst.nano).build())
                } catch (ignored: Exception) {}
            }
            // Preserve statusFrom/statusTo for CORRECT_STATUS events
            val statusFromStr = (data["statusFrom"] as? String)
            if (!statusFromStr.isNullOrBlank()) {
                try { b.setStatusFrom(TrackerStatus.valueOf(statusFromStr.uppercase())) } catch (e: Exception) {}
            }
            val statusToStr = (data["statusTo"] as? String)
            if (!statusToStr.isNullOrBlank()) {
                try { b.setStatusTo(TrackerStatus.valueOf(statusToStr.uppercase())) } catch (e: Exception) {}
            }
            existingEvents.add(b.build())
        }

        // Build the new CORRECT_STATUS event with proper hash chain
        val targetEventId = java.util.UUID.randomUUID().toString()
        val incomingBuilder = TrackerEvent.newBuilder()
            .setId(targetEventId)
            .setApplicationId(applicationId)
            .setActor(EventActor.ACTOR_USER)
            .setType(TrackerEventType.CORRECT_STATUS)
            .setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(nowInstant.epochSecond).setNanos(nowInstant.nano).build())
        note?.let { incomingBuilder.setNote(it) }

        // Parse status enums gracefully — handle legacy/invalid status values
        try {
            incomingBuilder.setStatusFrom(TrackerStatus.valueOf(previousStatus.uppercase()))
        } catch (e: Exception) {
            // Legacy status value (e.g. "INTERVIEW" instead of "INTERVIEWING")
            incomingBuilder.setStatusFrom(TrackerStatus.TRACKER_STATUS_UNSPECIFIED)
        }
        try {
            incomingBuilder.setStatusTo(TrackerStatus.valueOf(newStatus.uppercase()))
        } catch (e: Exception) {
            t.println(red("Invalid target status: $newStatus (must be a valid TrackerStatus enum value)"))
            return
        }

        val chained = EventChainBuilder.computeNextLink(
            existingEvents = existingEvents,
            newEvent = incomingBuilder.build(),
            applicationId = applicationId
        )

        // 1. Write the chained event
        val eventData = mutableMapOf<String, Any?>(
            "eventId" to chained.id,
            "ts" to nowInstant.toString(),
            "type" to chained.type.name,
            "actor" to chained.actor.name,
            "sequenceNumber" to chained.sequenceNumber,
            "prevEventHash" to chained.prevEventHash,
            "eventHash" to chained.eventHash,
            "statusFrom" to previousStatus,
            "statusTo" to newStatus,
        )
        if (note != null) {
            eventData["note"] = note
        }
        eventsColl.document(chained.id).set(eventData).get()

        // 2. Update parent doc
        val isTerminal = when (newStatus.uppercase()) {
            "OFFER_DECLINED", "HIRED", "REJECTED", "WITHDRAWN", "NO_RESPONSE" -> true
            else -> false
        }

        docRef.update(
            mapOf(
                "status" to newStatus,
                "isOpen" to !isTerminal,
                "updatedAt" to now
            )
        ).get()

        t.println(green("✓ Successfully updated $applicationId status: $previousStatus -> $newStatus"))
    }
}
