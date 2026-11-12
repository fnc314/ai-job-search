package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import com.google.cloud.firestore.FieldValue
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider

public class MergeCommand : CliktCommand(name = "merge") {
    private val canonicalId: String by argument("canonical", help = "Canonical application slug ID (the record to keep)")
    private val duplicateId: String by argument("duplicate", help = "Duplicate application slug ID (the record to merge and delete)")
    private val dryRun: Boolean by option("--dry-run", help = "Show what would be merged without making changes").flag()
    private val note: String? by option("--note", help = "Optional context note for the merge event")

    override fun run() {
        val t = Terminal()
        if (!dryRun) {
            FirestoreProvider.assertWritesAllowed("merge applications")
        }

        val db = FirestoreProvider.firestore
        val appsCollection = db.collection("applications")
        val emailsCollection = db.collection("emails")

        val canonicalRef = appsCollection.document(canonicalId)
        val duplicateRef = appsCollection.document(duplicateId)

        // Fetch both documents
        val canonicalSnap = canonicalRef.get().get()
        val duplicateSnap = duplicateRef.get().get()

        if (!canonicalSnap.exists()) {
            t.println(red("Canonical application not found: $canonicalId"))
            return
        }
        if (!duplicateSnap.exists()) {
            t.println(red("Duplicate application not found: $duplicateId"))
            return
        }

        if (canonicalId == duplicateId) {
            t.println(yellow("Canonical and duplicate IDs are the same. Nothing to merge."))
            return
        }

        t.println(yellow("Merging '$duplicateId' into '$canonicalId'..."))

        // 1. Move emailMessageIds from duplicate to canonical
        val duplicateEmails = (duplicateSnap.get("emailMessageIds") as? List<*>)?.map { it.toString() } ?: emptyList()
        val canonicalEmails = (canonicalSnap.get("emailMessageIds") as? List<*>)?.map { it.toString() } ?: emptyList()

        val newEmails = duplicateEmails.filter { it !in canonicalEmails }
        if (newEmails.isNotEmpty()) {
            t.println(yellow("  → Moving ${newEmails.size} email(s) from duplicate to canonical"))
            for (emailId in newEmails) {
                t.println(yellow("    - $emailId"))
            }
        }

        if (dryRun) {
            t.println(green("✓ Dry run complete. No changes made."))
            t.println(yellow("Run without --dry-run to execute the merge (requires TRACKER_ALLOW_WRITES=1)"))
            return
        }

        // Execute the merge
        val updates = mutableMapOf<String, Any>()
        updates["updatedAt"] = Timestamp.now()

        // Add new email IDs to canonical
        if (newEmails.isNotEmpty()) {
            updates["emailMessageIds"] = FieldValue.arrayUnion(*newEmails.toTypedArray())
        }

        // Update email documents to point to canonical application
        for (emailId in newEmails) {
            val emailRef = emailsCollection.document(emailId)
            emailRef.update("applicationId", canonicalId).get()
        }

        // Copy events from duplicate to canonical
        val duplicateEventsSnap = duplicateRef.collection("events").get().get()
        val eventsToCopy = duplicateEventsSnap.size()
        for (eventSnap in duplicateEventsSnap.documents) {
            val eventData = eventSnap.data.toMutableMap()
            eventData["ts"] = Timestamp.now()
            eventData["note"] = (note ?: "") + (if (note.isNullOrBlank()) "" else " ") + "(merged from $duplicateId)"
            val newEventRef = canonicalRef.collection("events").document()
            newEventRef.set(eventData).get()
        }

        // Copy artifacts from duplicate to canonical
        val duplicateArtifactsSnap = duplicateRef.collection("artifacts").get().get()
        val artifactsToCopy = duplicateArtifactsSnap.size()
        for (artifactSnap in duplicateArtifactsSnap.documents) {
            val artifactData = artifactSnap.data.toMutableMap()
            artifactData["ts"] = Timestamp.now()
            val newArtifactRef = canonicalRef.collection("artifacts").document()
            newArtifactRef.set(artifactData).get()
        }

        // Add merge timeline event
        val mergeEventRef = canonicalRef.collection("events").document()
        val mergeEventData = mutableMapOf<String, Any>(
            "eventId" to mergeEventRef.id,
            "ts" to Timestamp.now(),
            "eventType" to "IMPORT",
            "actor" to "USER",
            "note" to "Merged duplicate application '$duplicateId' into this record" + (if (note.isNullOrBlank()) "" else ": $note"),
        )
        mergeEventRef.set(mergeEventData).get()

        // Update canonical document
        if (updates.isNotEmpty()) {
            canonicalRef.update(updates).get()
        }

        // Delete the duplicate
        duplicateRef.delete().get()

        t.println(green("✓ Merged '$duplicateId' into '$canonicalId'"))
        if (newEmails.isNotEmpty()) {
            t.println(green("  → ${newEmails.size} email(s) moved"))
        }
        if (eventsToCopy > 0) {
            t.println(green("  → $eventsToCopy event(s) copied"))
        }
        if (artifactsToCopy > 0) {
            t.println(green("  → $artifactsToCopy artifact(s) copied"))
        }
        t.println(green("  → Duplicate record deleted"))
    }
}
