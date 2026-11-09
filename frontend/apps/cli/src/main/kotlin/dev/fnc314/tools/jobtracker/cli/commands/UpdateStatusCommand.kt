package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider

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

        // 1. Append timeline event
        val eventDoc = docRef.collection("events").document()
        val eventData = mutableMapOf<String, Any>(
            "eventId" to eventDoc.id,
            "ts" to now,
            "eventType" to "CORRECT_STATUS",
            "actor" to "USER",
            "statusFrom" to previousStatus,
            "statusTo" to newStatus,
        )
        if (note != null) {
            eventData["note"] = note!!
        }
        eventDoc.set(eventData).get()

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
