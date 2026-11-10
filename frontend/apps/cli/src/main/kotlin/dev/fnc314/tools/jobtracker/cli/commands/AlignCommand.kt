package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobsearch.parser.ApplicationTimelineProjector
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEvent
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider
import java.time.Instant

public class AlignCommand : CliktCommand(name = "align") {
    private val applicationId: String? by argument("id", help = "Optional target application ID to align a single record").optional()
    private val dryRun: Boolean by option("--dry-run", help = "Report only without writing changes").flag(default = false)

    override fun run() {
        val t = Terminal()
        t.println(cyan("=== Aligning Job Application Records ==="))

        if (!dryRun) {
            FirestoreProvider.assertWritesAllowed("application alignment")
        } else {
            t.println(yellow("[DRY RUN MODE] Changes will not be persisted to Firestore."))
        }

        val db = FirestoreProvider.firestore
        val appsToProcess = if (!applicationId.isNullOrBlank()) {
            val doc = db.collection("applications").document(applicationId!!).get().get()
            if (!doc.exists()) {
                t.println(yellow("Application not found: $applicationId"))
                return
            }
            listOf(doc)
        } else {
            db.collection("applications").get().get().documents
        }

        var alignedCount = 0

        for (appDoc in appsToProcess) {
            val eventsDocs = appDoc.reference.collection("events").get().get().documents
            val events = eventsDocs.map { mapDocToTrackerEvent(it.id, it.data) }

            val timeline = ApplicationTimelineProjector.projectTimeline(events)
            val confirmation = ApplicationTimelineProjector.projectSubmissionConfirmation(null, events)
            val counters = ApplicationTimelineProjector.projectCounters(events)
            val notesRendered = ApplicationTimelineProjector.projectNotes(events)
            val latestStatus = ApplicationTimelineProjector.latestStatus(events)

            val updates = mutableMapOf<String, Any?>(
                "notesRendered" to notesRendered,
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
                "counters" to counters,
                "schemaVersion" to 10
            )

            if (latestStatus != null && latestStatus != TrackerStatus.TRACKER_STATUS_UNSPECIFIED) {
                updates["status"] = latestStatus.name
            }

            alignedCount++
            if (!dryRun) {
                updates["updatedAt"] = Timestamp.now()
                appDoc.reference.set(updates, com.google.cloud.firestore.SetOptions.merge()).get()
            }
        }

        t.println(green("✓ Aligned $alignedCount application record(s)."))
    }

    private fun mapDocToTrackerEvent(id: String, data: Map<String, Any?>): TrackerEvent {
        val b = TrackerEvent.newBuilder().setId(id)
        val typeStr = (data["type"] as? String) ?: (data["eventType"] as? String) ?: "ADD_NOTE"
        try {
            b.setType(TrackerEventType.valueOf(typeStr.uppercase()))
        } catch (_: Exception) {
            b.setType(TrackerEventType.ADD_NOTE)
        }
        val actorStr = (data["actor"] as? String) ?: "ACTOR_USER"
        try {
            b.setActor(EventActor.valueOf(actorStr.uppercase()))
        } catch (_: Exception) {
            b.setActor(EventActor.ACTOR_USER)
        }
        (data["statusTo"] as? String)?.let { sTo ->
            try {
                b.setStatusTo(TrackerStatus.valueOf(sTo.uppercase()))
            } catch (_: Exception) {}
        }
        (data["sequenceNumber"] as? Number)?.let { b.setSequenceNumber(it.toLong()) }
        (data["eventHash"] as? String)?.let { b.setEventHash(it) }
        (data["prevEventHash"] as? String)?.let { b.setPrevEventHash(it) }
        (data["note"] as? String)?.let { b.setNote(it) }
        val tsStr = (data["ts"] as? String) ?: (data["date"] as? String)
        if (!tsStr.isNullOrBlank()) {
            try {
                val inst = Instant.parse(tsStr)
                b.setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(inst.epochSecond).setNanos(inst.nano).build())
            } catch (_: Exception) {}
        }
        return b.build()
    }
}
