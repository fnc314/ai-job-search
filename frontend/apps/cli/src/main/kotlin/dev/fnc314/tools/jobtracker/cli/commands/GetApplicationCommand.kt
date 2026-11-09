package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider

public class GetApplicationCommand : CliktCommand(name = "get") {
    private val applicationId: String by argument("id", help = "Application slug ID")

    override fun run() {
        val t = Terminal()
        val db = FirestoreProvider.firestore
        val docRef = db.collection("applications").document(applicationId)
        val snap = docRef.get().get()

        if (!snap.exists()) {
            t.println(yellow("Application not found: $applicationId"))
            return
        }

        val data = snap.data ?: emptyMap<String, Any?>()
        t.println(cyan("=== Application Details: $applicationId ==="))
        t.println("Company:       ${data["company"]}")
        t.println("Role:          ${data["role"]}")
        t.println("Status:        ${data["status"]}")
        t.println("Channel:       ${data["channel"]}")
        t.println("Location:      ${data["location"]}")
        t.println("Fit Rating:    ${data["fitRating"] ?: "-"}")
        t.println("Job URL:       ${data["jobUrl"] ?: "-"}")
        t.println("App URL:       ${data["jobApplicationUrl"] ?: "-"}")
        t.println("UUID:          ${data["uuid"] ?: "-"}")

        val emailIds = (data["emailMessageIds"] as? List<*>) ?: emptyList<Any>()
        t.println("\n" + cyan("--- Linked Email Messages (${emailIds.size}) ---"))
        if (emailIds.isEmpty()) {
            t.println(gray("No linked email messages."))
        } else {
            emailIds.forEach { emailId ->
                t.println("  • ${green(emailId.toString())}")
            }
        }

        val eventsSnap = docRef.collection("events").orderBy("ts").get().get()
        t.println("\n" + cyan("--- Timeline Events (${eventsSnap.size()}) ---"))
        if (eventsSnap.isEmpty) {
            t.println(gray("No events recorded."))
        } else {
            val eventsTbl = table {
                header {
                    row(cyan("Date/Time"), cyan("Event Type"), cyan("Actor"), cyan("Email Message ID"), cyan("Notes"))
                }
                body {
                    eventsSnap.documents.forEach { eventDoc ->
                        val ev = eventDoc.data
                        val ts = ev["ts"]?.toString() ?: ""
                        val type = ev["eventType"]?.toString() ?: ""
                        val actor = ev["actor"]?.toString() ?: ""
                        val emailMsgId = ev["emailMessageId"]?.toString() ?: "-"
                        val note = ev["note"]?.toString() ?: ""
                        row(ts, type, actor, emailMsgId, note)
                    }
                }
            }
            t.println(eventsTbl)
        }
    }
}
