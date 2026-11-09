package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

public class BackfillEmailsCommand : CliktCommand(name = "emails") {
    private val applyWrites: Boolean by option("--apply", help = "Apply updates to Firestore (default: dry-run)").flag(default = false)
    private val stateFilePath: String? by option("--state-file", help = "Path to gmail-sync/state.json")
    private val inboxFilePath: String? by option("--inbox-file", help = "Path to generated/gmail-inbox.json")
    private val jobLabelsFilePath: String? by option("--job-labels-file", help = "Path to generated/gmail-job-labels.json")

    private data class InboxMessage(
        val messageId: String,
        val subject: String,
        val sender: String,
        val snippet: String,
        val receivedAt: String
    )

    override fun run() {
        val t = Terminal()
        t.println(cyan("=== Historical Email Association & Metadata Matching ==="))

        if (applyWrites) {
            FirestoreProvider.assertWritesAllowed("historical email backfill")
        } else {
            t.println(yellow("[DRY RUN MODE] Run with --apply and TRACKER_ALLOW_WRITES=1 to write changes to Firestore."))
        }

        val db = FirestoreProvider.firestore

        // 1. Ingest JOB/* label hierarchy from gmail-job-labels.json
        val jobLabelsFile = File(jobLabelsFilePath ?: "generated/gmail-job-labels.json")
        val knownJobLabels = mutableSetOf<String>()
        val firmLabels = mutableMapOf<String, String>() // normalized firm token -> label name

        if (jobLabelsFile.exists()) {
            try {
                val parsed = Json.parseToJsonElement(jobLabelsFile.readText()).jsonObject
                val labelsArray = parsed["labels"]?.jsonArray
                labelsArray?.forEach {
                    val label = it.jsonPrimitive.content
                    knownJobLabels.add(label)
                    if (label.startsWith("JOB/")) {
                        val firm = label.removePrefix("JOB/").lowercase().replace(" ", "").replace("-", "")
                        firmLabels[firm] = label
                    }
                }
                t.println(green("Loaded ${knownJobLabels.size} JOB labels (${firmLabels.size} firm-specific) from ${jobLabelsFile.path}"))
            } catch (e: Exception) {
                t.println(yellow("Warning: Could not parse ${jobLabelsFile.path}: ${e.message}"))
            }
        } else {
            t.println(gray("No job-labels file found at ${jobLabelsFile.path} (proceeding without firm label index)"))
        }

        // 2. Ingest messages from generated/gmail-inbox.json
        val inboxFile = File(inboxFilePath ?: "generated/gmail-inbox.json")
        val inboxMessages = mutableListOf<InboxMessage>()

        if (inboxFile.exists()) {
            try {
                val parsed = Json.parseToJsonElement(inboxFile.readText()).jsonObject
                val messagesArray = parsed["messages"]?.jsonArray
                messagesArray?.forEach {
                    val obj = it.jsonObject
                    val messageId = obj["messageId"]?.jsonPrimitive?.content ?: ""
                    if (messageId.isNotBlank()) {
                        inboxMessages.add(
                            InboxMessage(
                                messageId = messageId,
                                subject = obj["subject"]?.jsonPrimitive?.content ?: "",
                                sender = obj["sender"]?.jsonPrimitive?.content ?: "",
                                snippet = obj["snippet"]?.jsonPrimitive?.content ?: "",
                                receivedAt = obj["receivedAt"]?.jsonPrimitive?.content ?: ""
                            )
                        )
                    }
                }
                t.println(green("Loaded ${inboxMessages.size} cached inbox messages from ${inboxFile.path}"))
            } catch (e: Exception) {
                t.println(yellow("Warning: Could not parse ${inboxFile.path}: ${e.message}"))
            }
        } else {
            t.println(gray("No inbox file found at ${inboxFile.path} (skipping inbox metadata correlation)"))
        }

        // 3. Scan Firestore applications, events subcollections, and correlate inbox messages
        val appsSnap = db.collection("applications").get().get()
        t.println("Scanning ${appsSnap.size()} Firestore applications for event and metadata correlation...\n")

        var totalEventsWithEmail = 0
        var totalAppsUpdated = 0
        var totalLinksCreated = 0
        var totalInboxMatches = 0

        val applicationCues = listOf(
            "application", "applied", "applying", "thank you for your interest",
            "thank you for applying", "update on your", "received your application",
            "we got your application", "closed your application", "interview",
            "meet and greet", "next steps", "talent community", "role at ",
            "mobile role", "invitation to interview", "offer", "regarding your application"
        )

        val noisePatterns = listOf(
            "receipt", "order receipt", "payment", "refund", "invoice",
            "deposit", "account linking", "digital wallet", "statement",
            "security alert", "sign-in", "free cloud usage", "cookies",
            "see hiring trends", "impressions last week", "posts got",
            "new jobs similar to", "alert for", "may want to hire you",
            "are you still interested in these jobs", "talent scout digest"
        )

        for (appDoc in appsSnap.documents) {
            val appId = appDoc.id
            val appData = appDoc.data
            val company = (appData["company"] as? String)?.trim() ?: ""
            val existingEmailIds = ((appData["emailMessageIds"] as? List<*>) ?: emptyList<Any>())
                .map { it.toString() }
                .toMutableSet()

            val initialSize = existingEmailIds.size
            val normalizedCompany = company.lowercase().replace(" ", "").replace("-", "")
            val associatedJobLabel = firmLabels[normalizedCompany]

            // 3a. Discover timeline events with emailMessageId
            val eventsSnap = appDoc.reference.collection("events").get().get()
            for (evDoc in eventsSnap.documents) {
                val emailId = evDoc.getString("emailMessageId")
                if (!emailId.isNullOrBlank()) {
                    totalEventsWithEmail++
                    existingEmailIds.add(emailId)
                }
            }

            // 3b. Correlate against inbox messages using exact word boundary company matching and application cues
            if (company.length >= 3 && inboxMessages.isNotEmpty()) {
                val companyRegex = Regex("\\b${Regex.escape(company)}\\b", RegexOption.IGNORE_CASE)
                for (msg in inboxMessages) {
                    val subLower = msg.subject.lowercase()
                    val senderLower = msg.sender.lowercase()
                    val bodyLower = msg.snippet.lowercase()

                    // Exclude billing/promotional noise
                    val isNoise = noisePatterns.any { subLower.contains(it) }
                    if (isNoise) continue

                    val hasApplicationCue = applicationCues.any { subLower.contains(it) || bodyLower.contains(it) }
                    if (!hasApplicationCue) continue

                    val isCompanyInSubject = companyRegex.containsMatchIn(msg.subject)
                    val isCompanyInSender = companyRegex.containsMatchIn(msg.sender)

                    val matches = isCompanyInSubject || isCompanyInSender

                    if (matches) {
                        val added = existingEmailIds.add(msg.messageId)
                        if (added) {
                            totalInboxMatches++
                            t.println(
                                "  • Matched [${cyan(msg.messageId)}] -> ${bold(company)} " +
                                    "${if (associatedJobLabel != null) brightBlue("($associatedJobLabel)") else ""} " +
                                    dim("Subj: ${msg.subject.take(65)}")
                            )
                        }
                    }
                }
            }

            val newlyAdded = existingEmailIds.size - initialSize
            if (newlyAdded > 0) {
                totalAppsUpdated++
                totalLinksCreated += newlyAdded
                t.println("  ✓ $appId: associating $newlyAdded email message IDs (total linked: ${existingEmailIds.size})")

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
        t.println("Timeline events found with email IDs: $totalEventsWithEmail")
        t.println("Inbox messages correlated via metadata: $totalInboxMatches")
        t.println("Total applications updated: $totalAppsUpdated")
        t.println("Total email associations established: $totalLinksCreated")

        if (applyWrites) {
            t.println(green("✓ Successfully committed backfilled email associations to Firestore."))
        } else {
            t.println(yellow("Dry run complete. No modifications were written to Firestore."))
        }
    }
}
