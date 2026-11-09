package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import com.google.cloud.firestore.FieldValue
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

public class BackfillEmailsCommand : CliktCommand(name = "emails") {
    private val applyWrites: Boolean by option("--apply", help = "Apply updates to Firestore (default: dry-run)").flag(default = false)
    private val stateFilePath: String? by option("--state-file", help = "Path to gmail-sync/state.json")

    override fun run() {
        val t = Terminal()
        t.println(cyan("=== Retroactive Historical Email Backfill ==="))

        if (applyWrites) {
            FirestoreProvider.assertWritesAllowed("historical email backfill")
        } else {
            t.println(yellow("[DRY RUN MODE] Run with --apply and TRACKER_ALLOW_WRITES=1 to write changes to Firestore."))
        }

        val db = FirestoreProvider.firestore

        // 1. Read processed message IDs from state.json
        val stateFile = if (stateFilePath != null) {
            File(stateFilePath!!)
        } else {
            File("gmail-sync/state.json")
        }

        val stateMessageIds = mutableSetOf<String>()
        if (stateFile.exists()) {
            try {
                val parsed = Json.parseToJsonElement(stateFile.readText()).jsonObject
                val msgArray = parsed["processed_message_ids"]?.jsonArray
                msgArray?.forEach {
                    stateMessageIds.add(it.jsonPrimitive.content)
                }
                t.println(green("Loaded ${stateMessageIds.size} processed message IDs from ${stateFile.path}"))
            } catch (e: Exception) {
                t.println(yellow("Warning: Could not parse ${stateFile.path}: ${e.message}"))
            }
        } else {
            t.println(gray("No state file found at ${stateFile.path} (skipping state file correlation)"))
        }

        // 2. Discover timeline events with emailMessageId
        val appsSnap = db.collection("applications").get().get()
        t.println("Scanning ${appsSnap.size()} application records and their timeline events...")

        var totalEventsWithEmail = 0
        var totalAppsUpdated = 0
        var totalLinksCreated = 0

        for (appDoc in appsSnap.documents) {
            val appId = appDoc.id
            val appData = appDoc.data
            val existingEmailIds = ((appData["emailMessageIds"] as? List<*>) ?: emptyList<Any>())
                .map { it.toString() }
                .toMutableSet()

            val initialSize = existingEmailIds.size

            // Read events subcollection
            val eventsSnap = appDoc.reference.collection("events").get().get()
            for (evDoc in eventsSnap.documents) {
                val emailId = evDoc.getString("emailMessageId")
                if (!emailId.isNullOrBlank()) {
                    totalEventsWithEmail++
                    existingEmailIds.add(emailId)
                }
            }

            val newlyAdded = existingEmailIds.size - initialSize
            if (newlyAdded > 0) {
                totalAppsUpdated++
                totalLinksCreated += newlyAdded
                t.println("  • $appId: associating $newlyAdded email message IDs (total: ${existingEmailIds.size})")

                if (applyWrites) {
                    appDoc.reference.update(
                        mapOf(
                            "emailMessageIds" to existingEmailIds.toList(),
                            "updatedAt" to Timestamp.now()
                        )
                    ).get()
                }
            }
        }

        t.println("\n" + cyan("=== Backfill Summary ==="))
        t.println("Total timeline events found with email IDs: $totalEventsWithEmail")
        t.println("Total applications with updated email associations: $totalAppsUpdated")
        t.println("Total new email links established: $totalLinksCreated")

        if (applyWrites) {
            t.println(green("✓ Successfully committed backfilled email associations to Firestore."))
        } else {
            t.println(yellow("Dry run complete. No modifications were written to Firestore."))
        }
    }
}
