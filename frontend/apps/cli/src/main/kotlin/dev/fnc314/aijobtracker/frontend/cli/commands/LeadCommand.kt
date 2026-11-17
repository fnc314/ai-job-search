package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import com.google.cloud.firestore.SetOptions
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import dev.fnc314.aijobtracker.frontend.core.data.model.Uuid7
import dev.fnc314.aijobtracker.records.parser.DocumentIdGenerator
import java.time.Instant

/**
 * Command to record a recruiter outreach lead in Firestore.
 */
public class LeadCommand : CliktCommand(name = "lead") {
    private val company: String by option("-c", "--company", help = "Company name (required)").required()
    private val channel: String? by option("--channel", help = "Channel value (default: OUTREACH_LINKEDIN)")
    private val contact: String? by option("-r", "--contact", help = "Recruiter or contact name")
    private val notes: List<String> by option("-n", "--note", help = "Context about outreach").multiple()
    private val tags: List<String> by option("-t", "--tag", help = "Additional tags").multiple()
    private val dryRun: Boolean by option("--dry-run", help = "Resolve and validate only").flag(default = false)

    override fun run() {
        val t = Terminal()
        if (!dryRun) {
            RemoteDataSourceProvider.assertWritesAllowed("lead record creation")
        }

        val companyId = DocumentIdGenerator.slug(company, fallback = "unknown-company")
        val applicationId = companyId // leads default to companyId
        val chosenChannel = channel?.uppercase() ?: "OUTREACH_LINKEDIN"
        val allTags = (listOf("process:recruiter:outreach") + tags).distinct()

        val now = Timestamp.now()
        val nowIso = Instant.now().toString()
        val today = nowIso.substringBefore("T")

        val docData = mutableMapOf<String, Any?>(
            "id" to applicationId,
            "date" to today,
            "company" to company,
            "companyId" to companyId,
            "role" to "Recruiter Outreach Lead",
            "channel" to chosenChannel,
            "status" to "LEAD",
            "statusRank" to 0,
            "isOpen" to true,
            "tags" to allTags,
            "uuid" to Uuid7.generate().toString(),
            "schemaVersion" to 13,
            "createdAt" to now,
            "updatedAt" to now,
            "metadata" to emptyMap<String, Any>(),
            "contactIds" to emptyList<String>(),
            "notesRendered" to notes.joinToString("\n\n")
        )
        if (contact != null) {
            docData["contactPerson"] = contact!!
        }

        if (dryRun) {
            t.println(cyan("DRY RUN: would write lead application for '$company' ($applicationId):"))
            t.println(yellow("  Company: $company (ID: $companyId)"))
            t.println(yellow("  Channel: $chosenChannel"))
            if (contact != null) t.println(yellow("  Contact: $contact"))
            t.println(yellow("  Tags: ${allTags.joinToString(", ")}"))
            t.println(yellow("  Notes: ${notes.size} attached"))
            return
        }

        val db = RemoteDataSourceProvider.remote
        val docRef = db.collection("applications").document(applicationId)
        val snap = docRef.get().get()

        if (snap.exists()) {
            val existing = snap.data
            if (existing?.get("status") == "LEAD") {
                t.println(yellow("Lead for $company already exists at $applicationId."))
                return
            }
        }

        docRef.set(docData, SetOptions.merge()).get()

        // Upsert canonical company record
        db.collection("companies").document(companyId).set(
            mapOf(
                "companyId" to companyId,
                "name" to company,
                "updatedAt" to now
            ),
            SetOptions.merge()
        ).get()

        // Append initial outreach event
        val eventDoc = docRef.collection("events").document()
        val eventData = mutableMapOf<String, Any?>(
            "eventId" to eventDoc.id,
            "ts" to now,
            "date" to today,
            "eventType" to "RECRUITER_OUTREACH",
            "actor" to "USER",
            "statusTo" to "LEAD",
            "note" to (notes.firstOrNull() ?: "Initial recruiter outreach recorded")
        )
        eventDoc.set(eventData).get()

        t.println(green("✓ Successfully recorded recruiter outreach lead: $applicationId ($company)"))
    }
}
