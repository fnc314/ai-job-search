package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider
import java.util.regex.Pattern

/**
 * Scans emails classified as CORRESPONDENCE and re-classifies them using improved patterns.
 * Updates both the email status and the application status when a better classification is found.
 */
public class ReclassifyCommand : CliktCommand(name = "reclassify") {
    private val dryRun: Boolean by option("--dry-run", help = "Show what would be changed without making changes").flag()

    // Rejection patterns that were previously missed
    private val rejectionPatterns = listOf(
        Pattern.compile("\\bunfortunately\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("not moving forward", Pattern.CASE_INSENSITIVE),
        Pattern.compile("regret to inform", Pattern.CASE_INSENSITIVE),
        Pattern.compile("no longer under consideration", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bnot selected\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("won't be moving forward", Pattern.CASE_INSENSITIVE),
        Pattern.compile("will not be moving forward", Pattern.CASE_INSENSITIVE),
        Pattern.compile("decided to (?:move forward|proceed) with other", Pattern.CASE_INSENSITIVE),
        Pattern.compile("decision to (?:move forward|proceed) with other", Pattern.CASE_INSENSITIVE),
        Pattern.compile("move forward with other candidates", Pattern.CASE_INSENSITIVE),
        Pattern.compile("difficult decision", Pattern.CASE_INSENSITIVE),
        Pattern.compile("other candidates were more aligned", Pattern.CASE_INSENSITIVE),
        Pattern.compile("application status", Pattern.CASE_INSENSITIVE),
        Pattern.compile("position (?:has been|was) filled", Pattern.CASE_INSENSITIVE),
    )

    private val offerPatterns = listOf(
        Pattern.compile("\\bjob offer\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("pleased to offer", Pattern.CASE_INSENSITIVE),
        Pattern.compile("offer letter", Pattern.CASE_INSENSITIVE),
        Pattern.compile("we would like to offer", Pattern.CASE_INSENSITIVE),
    )

    private val interviewPatterns = listOf(
        Pattern.compile("\\binterview\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("schedule (?:a )?(?:call|chat|conversation)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("phone screen", Pattern.CASE_INSENSITIVE),
    )

    override fun run() {
        val t = Terminal()
        if (!dryRun) {
            FirestoreProvider.assertWritesAllowed("reclassify emails")
        }

        val db = FirestoreProvider.firestore
        val emailsCollection = db.collection("emails")
        val appsCollection = db.collection("applications")

        // Fetch all CORRESPONDENCE emails
        val emailsSnap = emailsCollection.get().get()
        var totalScanned = 0
        var reclassified = 0

        for (doc in emailsSnap.documents) {
            val data = doc.data ?: continue
            val currentStatus = data["status"] as? String ?: continue
            if (currentStatus != "CORRESPONDENCE") continue

            totalScanned++
            val subject = (data["subject"] as? String) ?: ""
            val snippet = (data["snippet"] as? String) ?: ""
            val appId = data["applicationId"] as? String ?: continue
            val combined = "$subject $snippet"

            // Classify using improved patterns
            val classification = classify(combined)
            if (classification == null) continue

            val (eventType, statusTo) = classification
            t.println(yellow("Email ${doc.id}: ${subject.take(60)} -> $eventType/$statusTo (app: $appId)"))

            if (dryRun) {
                reclassified++
                continue
            }

            // Update email status
            doc.reference.update("status", eventType, "updatedAt", Timestamp.now()).get()

            // Update application status if needed
            val appSnap = appsCollection.document(appId).get().get()
            if (!appSnap.exists()) {
                t.println(yellow("  ✗ Application $appId not found, skipping"))
                continue
            }

            val appData = appSnap.data ?: continue
            val currentAppStatus = appData["status"] as? String ?: continue
            if (currentAppStatus == statusTo) {
                t.println(yellow("  - Application already at $statusTo, no change needed"))
                continue
            }

            val isTerminal = statusTo in listOf("REJECTED", "APPLICATION_WITHDRAWN", "OFFER_DECLINED", "HIRED")
            appsCollection.document(appId).update(
                "status", statusTo,
                "isOpen", !isTerminal,
                "updatedAt", Timestamp.now()
            ).get()
            t.println(green("  ✓ Application status updated: $currentAppStatus -> $statusTo"))

            // Add timeline event
            val eventRef = appsCollection.document(appId).collection("events").document()
            eventRef.set(mapOf(
                "eventId" to eventRef.id,
                "ts" to Timestamp.now(),
                "eventType" to eventType,
                "actor" to "SYSTEM",
                "statusFrom" to currentAppStatus,
                "statusTo" to statusTo,
                "emailMessageId" to doc.id,
                "note" to "Auto-reclassified from CORRESPONDENCE by reclassify command"
            )).get()
            t.println(green("  ✓ Timeline event added"))

            reclassified++
        }

        if (dryRun) {
            t.println(green("✓ Dry run: scanned $totalScanned CORRESPONDENCE emails, $reclassified would be reclassified"))
        } else {
            t.println(green("✓ Scanned $totalScanned CORRESPONDENCE emails, $reclassified reclassified"))
        }
    }

    private fun classify(combined: String): Pair<String, String>? {
        // Check rejection first (highest priority)
        if (rejectionPatterns.any { it.matcher(combined).find() }) {
            return "REJECT" to "REJECTED"
        }
        if (offerPatterns.any { it.matcher(combined).find() }) {
            return "OFFER" to "OFFER"
        }
        if (interviewPatterns.any { it.matcher(combined).find() }) {
            return "SCHEDULE_INTERVIEW" to "INTERVIEWING"
        }
        return null
    }
}
