package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider

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

        // Append a timeline event recording the edit
        val eventDoc = docRef.collection("events").document()
        val eventData = mutableMapOf<String, Any>(
            "eventId" to eventDoc.id,
            "ts" to Timestamp.now(),
            "eventType" to "CORRECT_STATUS",
            "actor" to "USER",
        )
        val n = note
        if (n != null) {
            eventData["note"] = n
        }
        if (s != null) {
            eventData["statusFrom"] = snap.getString("status") ?: "UNKNOWN"
            eventData["statusTo"] = s
        }
        eventDoc.set(eventData).get()

        // Update the application document
        docRef.update(updates).get()

        t.println(green("✓ Successfully updated $applicationId: ${updates.keys.filter { it != "updatedAt" }.joinToString(", ")}"))
    }
}
