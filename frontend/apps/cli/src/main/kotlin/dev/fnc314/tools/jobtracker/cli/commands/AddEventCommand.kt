package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import com.google.cloud.firestore.FieldValue
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider

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

        val now = Timestamp.now()
        val eventDoc = docRef.collection("events").document()
        val eventData = mutableMapOf<String, Any>(
            "eventId" to eventDoc.id,
            "ts" to now,
            "eventType" to eventType,
            "actor" to "USER"
        )
        if (note != null) {
            eventData["note"] = note!!
        }
        if (emailMessageId != null) {
            eventData["emailMessageId"] = emailMessageId!!
        }

        eventDoc.set(eventData).get()

        // If emailMessageId is provided, also associate to parent doc atomically
        if (emailMessageId != null) {
            docRef.update(
                mapOf(
                    "emailMessageIds" to FieldValue.arrayUnion(emailMessageId),
                    "updatedAt" to now
                )
            ).get()
        } else {
            docRef.update("updatedAt", now).get()
        }

        t.println(green("✓ Successfully appended event $eventType to $applicationId${if (emailMessageId != null) " (linked email: $emailMessageId)" else ""}"))
    }
}
