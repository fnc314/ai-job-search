package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider

public class UpdateScoreCommand : CliktCommand(name = "score") {
    private val applicationId: String by argument("id", help = "Application slug ID")
    private val score: Int by argument("score", help = "Recalculated fit score (0-100)").int()

    override fun run() {
        val t = Terminal()
        FirestoreProvider.assertWritesAllowed("score recalculation")

        if (score !in 0..100) {
            t.println(red("Score must be between 0 and 100 inclusive. Received: $score"))
            return
        }

        val db = FirestoreProvider.firestore
        val docRef = db.collection("applications").document(applicationId)
        val snap = docRef.get().get()

        if (!snap.exists()) {
            t.println(red("Application not found: $applicationId"))
            return
        }

        val prevScore = snap.getLong("fitRating")?.toInt() ?: -1
        val now = Timestamp.now()

        // 1. Append timeline event
        val eventDoc = docRef.collection("events").document()
        val eventData = mapOf<String, Any>(
            "eventId" to eventDoc.id,
            "ts" to now,
            "eventType" to "RECALCULATE_FIT",
            "actor" to "USER",
            "note" to "Recalculated fit score: $prevScore -> $score"
        )
        eventDoc.set(eventData).get()

        // 2. Update parent doc
        docRef.update(
            mapOf(
                "fitRating" to score,
                "updatedAt" to now
            )
        ).get()

        t.println(green("✓ Successfully updated $applicationId fit rating: $prevScore -> $score"))
    }
}
