package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobsearch.parser.EventChainBuilder
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider
import java.time.Instant

public class EditCommand : CliktCommand(name = "edit") {
    private val applicationId: String by argument("id", help = "Application slug ID")
    private val company: String? by option("--company", help = "New company name")
    private val role: String? by option("--role", help = "New role title")
    private val status: String? by option("--status", help = "New funnel status")
    private val channel: String? by option("--channel", help = "Application channel")
    private val jobUrl: String? by option("--job-url", help = "Job posting URL")
    private val note: String? by option("--note", help = "Optional context note for the event")

    override fun run() {
        val t = Terminal()
        FirestoreProvider.assertWritesAllowed("edit application")

        val db = FirestoreProvider.firestore
        val docRef = db.collection("applications").document(applicationId)
        val snap = docRef.get().get()

        if (!snap.exists()) {
            t.println(red("Application not found: $applicationId"))
            return
        }

        val updates = mutableMapOf<String, Any>()

        if (company != null) {
            updates["company"] = company!!
        }
        if (role != null) {
            updates["role"] = role!!
        }
        if (channel != null) {
            updates["channel"] = channel!!
        }
        if (jobUrl != null) {
            updates["jobUrl"] = jobUrl!!
        }
        val s = status
        if (s != null) {
            val isTerminal = when (s.uppercase()) {
                "OFFER_DECLINED", "HIRED", "REJECTED", "WITHDRAWN", "NO_RESPONSE" -> true
                else -> false
            }
            updates["status"] = s
            updates["isOpen"] = !isTerminal
        }

        if (updates.isEmpty()) {
            t.println(yellow("No changes specified. Use --company, --role, --status, --channel, or --job-url."))
            return
        }

        updates["updatedAt"] = Timestamp.now()

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

        // Build the new event with proper hash chain
        val now = Timestamp.now()
        val nowInstant = Instant.ofEpochSecond(now.seconds, now.nanos.toLong())
        val targetEventId = java.util.UUID.randomUUID().toString()

        val eventType = if (s != null) TrackerEventType.CORRECT_STATUS else TrackerEventType.ADD_NOTE
        val incomingBuilder = TrackerEvent.newBuilder()
            .setId(targetEventId)
            .setApplicationId(applicationId)
            .setActor(EventActor.ACTOR_USER)
            .setType(eventType)
            .setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(nowInstant.epochSecond).setNanos(nowInstant.nano).build())
        note?.let { incomingBuilder.setNote(it) }
        if (s != null) {
            val previousStatus = snap.getString("status") ?: "UNKNOWN"
            incomingBuilder.setStatusFrom(TrackerStatus.valueOf(previousStatus.uppercase()))
            incomingBuilder.setStatusTo(TrackerStatus.valueOf(s.uppercase()))
        }

        val chained = EventChainBuilder.computeNextLink(
            existingEvents = existingEvents,
            newEvent = incomingBuilder.build(),
            applicationId = applicationId
        )

        // Write the chained event
        val eventData = mutableMapOf<String, Any?>(
            "eventId" to chained.id,
            "ts" to nowInstant.toString(),
            "type" to chained.type.name,
            "actor" to chained.actor.name,
            "sequenceNumber" to chained.sequenceNumber,
            "prevEventHash" to chained.prevEventHash,
            "eventHash" to chained.eventHash
        )
        val n = note
        if (n != null) {
            eventData["note"] = n
        }
        if (s != null) {
            eventData["statusFrom"] = snap.getString("status") ?: "UNKNOWN"
            eventData["statusTo"] = s
        }
        eventsColl.document(chained.id).set(eventData).get()

        // Update the application document
        docRef.set(updates, com.google.cloud.firestore.SetOptions.merge()).get()

        t.println(green("✓ Successfully updated $applicationId: ${updates.keys.filter { it != "updatedAt" }.joinToString(", ")}"))
    }
}
