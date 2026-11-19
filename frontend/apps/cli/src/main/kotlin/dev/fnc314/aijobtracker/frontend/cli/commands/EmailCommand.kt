package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.SetOptions
import dev.fnc314.aijobtracker.backend.BackendService
import dev.fnc314.aijobtracker.backend.classification.EmailClassificationResult
import dev.fnc314.aijobtracker.backend.classification.EmailClassifier
import dev.fnc314.aijobtracker.backend.classification.SourceEmail
import dev.fnc314.aijobtracker.backend.classification.StagedEmailChange
import dev.fnc314.aijobtracker.backend.classification.UnmatchedEmail
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * CLI command group for email ingestion, inbox reading, label syncing, and proposal staging.
 */
public class EmailCommand : CliktCommand(name = "email") {
    init {
        subcommands(
            EmailSyncLabelsCommand(),
            EmailReadInboxCommand(),
            EmailCollectCommand(),
            EmailDiscoverCommand(),
        )
    }

    override fun run(): Unit = Unit
}

internal class EmailSyncLabelsCommand : CliktCommand(name = "sync-labels") {
    private val verbose: Boolean by option("-v", "--verbose", help = "Print detailed progress").flag(default = false)
    private val label: String? by option("--label", help = "Fetch only this label/folder")
    private val since: String? by option("--since", help = "Lookback start date, YYYY-MM-DD")
    private val timeout: String? by option("--timeout", help = "Timeout in seconds")

    override fun run() {
        val t = Terminal()
        t.println(cyan("Syncing email labels via mail provider..."))

        val composioBin = findComposioBin()
        if (composioBin == null) {
            t.println(red("Error: composio binary not found in PATH (~/.local/bin/composio)"))
            return
        }

        try {
            val process = ProcessBuilder(
                composioBin, "execute", "GMAIL_LIST_LABELS", "-d", "{}"
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                t.println(yellow("Provider list labels output: $output"))
                t.println(red("Failed to list labels (exit code $exitCode)"))
                return
            }

            val labelsJson = extractJsonFromOutput(output)
            val generatedDir = getGeneratedDir()
            if (!generatedDir.exists()) generatedDir.mkdirs()

            File(generatedDir, "gmail-labels.json").writeText(labelsJson)

            val parsed = Json.parseToJsonElement(labelsJson)
            val rawLabels = when (parsed) {
                is JsonObject -> parsed["labels"]?.jsonArray ?: parsed["data"]?.jsonObject?.get("labels")?.jsonArray
                is JsonArray -> parsed
                else -> null
            } ?: JsonArray(emptyList())

            val jobLabels = rawLabels.filter { elem ->
                val name = elem.jsonObject["name"]?.jsonPrimitive?.contentOrNull ?: ""
                name.equals("JOB", ignoreCase = true) || name.startsWith("JOB/", ignoreCase = true)
            }

            File(generatedDir, "gmail-job-labels.json").writeText(
                Json.encodeToString(JsonArray(jobLabels))
            )

            // Update meta/sync in Firestore
            try {
                val db = RemoteDataSourceProvider.remote
                val labelNames = jobLabels.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }
                db.collection("meta").document("sync").set(
                    mapOf(
                        "gmail" to mapOf(
                            "labels" to labelNames,
                            "labelsSeenAt" to Instant.now().toString()
                        )
                    ),
                    SetOptions.merge()
                ).get()
            } catch (e: Exception) {
                t.println(yellow("Warning: could not update Firestore meta/sync: ${e.message}"))
            }

            t.println(green("✓ Successfully synced ${rawLabels.size} labels (${jobLabels.size} JOB labels cached)"))
        } catch (e: Exception) {
            t.println(red("Error executing sync-labels: ${e.message}"))
        }
    }
}

internal class EmailReadInboxCommand : CliktCommand(name = "read-inbox") {
    private val verbose: Boolean by option("-v", "--verbose", help = "Print detailed progress").flag(default = false)
    private val label: String? by option("--label", help = "Fetch only this label/folder")
    private val since: String? by option("--since", help = "Lookback start date, YYYY-MM-DD")
    private val max: String? by option("--max", help = "Total messages to collect")
    private val timeout: String? by option("--timeout", help = "Timeout in seconds")
    private val dryRun: Boolean by option("--dry-run", help = "Read email and compare labels, but write nothing").flag(default = false)

    override fun run() {
        val t = Terminal()
        t.println(cyan("Reading job emails from inbox..."))

        val composioBin = findComposioBin()
        if (composioBin == null) {
            t.println(red("Error: composio binary not found in PATH"))
            return
        }

        // Determine query: use JOB label hierarchy or default query
        val generatedDir = getGeneratedDir()
        if (!generatedDir.exists()) generatedDir.mkdirs()

        val jobLabelsFile = File(generatedDir, "gmail-job-labels.json")
        val labelQuery = if (label != null) {
            "label:$label"
        } else if (jobLabelsFile.exists()) {
            try {
                val parsed = Json.parseToJsonElement(jobLabelsFile.readText()).jsonArray
                val names = parsed.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }
                if (names.isNotEmpty()) {
                    names.joinToString(" OR ") { "label:\"$it\"" }
                } else {
                    "label:JOB"
                }
            } catch (_: Exception) {
                "label:JOB"
            }
        } else {
            "label:JOB"
        }

        val sinceQuery = if (!since.isNullOrBlank()) " after:$since" else ""
        val fullQuery = "($labelQuery)$sinceQuery"

        if (verbose) {
            t.println("Executing query: $fullQuery")
        }

        val maxResults = max?.toIntOrNull() ?: 50
        val requestBody = Json.encodeToString(
            JsonObject(
                mapOf(
                    "user_id" to JsonPrimitive("me"),
                    "query" to JsonPrimitive(fullQuery),
                    "max_results" to JsonPrimitive(maxResults),
                    "verbose" to JsonPrimitive(true),
                    "include_payload" to JsonPrimitive(true)
                )
            )
        )
        val args = listOf(
            composioBin, "execute", "GMAIL_FETCH_EMAILS", "-d", requestBody
        )

        try {
            val process = ProcessBuilder(args).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                t.println(yellow("Provider fetch messages output: $output"))
                t.println(red("Failed to read inbox (exit code $exitCode)"))
                return
            }

                        // Strip HTML utility to extract real body text before building snippets
            fun stripHtml(raw: String): String {
                if (raw.isBlank()) return ""
                val withoutStyles = raw.replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
                val textOnly = withoutStyles.replace(Regex("<[^>]+>"), " ")
                return textOnly.replace(Regex("&nbsp;"), " ")
                    .replace(Regex("&amp;"), "&")
                    .replace(Regex("&lt;"), "<")
                    .replace(Regex("&gt;"), ">")
                    .replace(Regex("&quot;"), "\"")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            }

            val inboxJson = extractJsonFromOutput(output)
            val parsed = Json.parseToJsonElement(inboxJson)
            val rawMessages = when (parsed) {
                is JsonObject -> parsed["messages"]?.jsonArray ?: parsed["data"]?.jsonObject?.get("messages")?.jsonArray
                is JsonArray -> parsed
                else -> null
            } ?: JsonArray(emptyList())

            // Normalize message attributes to standard schema
            val normalizedMessages = rawMessages.mapNotNull { elem ->
                val obj = elem.jsonObject
                val id = obj["messageId"]?.jsonPrimitive?.contentOrNull
                    ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val subj = obj["subject"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val sender = obj["sender"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val rawBody = obj["messageText"]?.jsonPrimitive?.contentOrNull
                    ?: obj["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val cleanBody = stripHtml(rawBody)
                val snippet = if (cleanBody.isNotBlank()) "$subj\n\n$cleanBody".take(800) else subj.take(800)
                val received = obj["messageTimestamp"]?.jsonPrimitive?.contentOrNull
                    ?: obj["receivedAt"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val labels = obj["labels"]?.jsonArray ?: obj["labelIds"]?.jsonArray ?: JsonArray(emptyList())

                JsonObject(
                    mapOf(
                        "messageId" to JsonPrimitive(id),
                        "subject" to JsonPrimitive(subj),
                        "sender" to JsonPrimitive(sender),
                        "snippet" to JsonPrimitive(snippet),
                        "receivedAt" to JsonPrimitive(received),
                        "labels" to labels,
                        "body" to JsonPrimitive(cleanBody)
                    )
                )
            }

            val payload = JsonObject(
                mapOf(
                    "lookbackFrom" to JsonPrimitive(since ?: ""),
                    "query" to JsonPrimitive(fullQuery),
                    "messages" to JsonArray(normalizedMessages)
                )
            )

            if (!dryRun) {
                File(generatedDir, "gmail-inbox.json").writeText(Json.encodeToString(payload))
                t.println(green("✓ Successfully fetched ${normalizedMessages.size} messages to generated/gmail-inbox.json"))
            } else {
                t.println(green("✓ [dry-run] Found ${normalizedMessages.size} messages"))
            }
        } catch (e: Exception) {
            t.println(red("Error reading inbox: ${e.message}"))
        }
    }
}

internal class EmailCollectCommand : CliktCommand(name = "collect") {
    private val jsonPath: String? by option("--json", help = "Inbox payload path")
    private val labelsPath: String? by option("--labels", help = "JSON inventory of email provider labels")
    private val trigger: String? by option("--trigger", help = "Trigger source")
    private val dryRun: Boolean by option("--dry-run", help = "Preview without writing").flag(default = false)

    override fun run() {
        val t = Terminal()
        t.println(cyan("Collecting and staging email proposals..."))

        if (!dryRun) {
            RemoteDataSourceProvider.assertWritesAllowed("email proposal collection")
        }

        val file = if (jsonPath != null) File(jsonPath) else File(getGeneratedDir(), "gmail-inbox.json")
        if (!file.exists()) {
            t.println(yellow("Inbox payload file not found: ${file.absolutePath}"))
            t.println("Run 'mise email:read-inbox' first.")
            return
        }

        val db = RemoteDataSourceProvider.remote
        val syncSnap = db.collection("meta").document("sync").get().get()
        val syncData = syncSnap.data ?: emptyMap<String, Any>()
        val gmailMeta = syncData["gmail"] as? Map<*, *> ?: emptyMap<String, Any>()
        val processedIds = (gmailMeta["processedMessageIds"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()

        val parsed = Json.parseToJsonElement(file.readText())
        val messagesArray = when (parsed) {
            is JsonObject -> parsed["messages"]?.jsonArray
            is JsonArray -> parsed
            else -> null
        } ?: JsonArray(emptyList())

        // Load applications from Firestore to match against
        val appsSnap = db.collection("applications").get().get()
        val applications = appsSnap.documents.associate { doc ->
            doc.id to (doc.data ?: emptyMap<String, Any?>())
        }

        val newMessages = mutableListOf<Map<String, Any?>>()
        var alreadyProcessed = 0

        for (elem in messagesArray) {
            val obj = elem.jsonObject
            val msgId = obj["id"]?.jsonPrimitive?.contentOrNull ?: obj["messageId"]?.jsonPrimitive?.contentOrNull ?: ""
            if (msgId in processedIds) {
                alreadyProcessed++
            } else {
                val map = mutableMapOf<String, Any?>()
                obj.forEach { (k, v) ->
                    map[k] = v.jsonPrimitive.contentOrNull
                }
                newMessages.add(map)
            }
        }

        t.println("Found ${messagesArray.size} total message(s) ($alreadyProcessed already processed, ${newMessages.size} new)")

        if (newMessages.isEmpty()) {
            t.println(green("No new messages to process."))
            return
        }

        val changes = mutableListOf<StagedEmailChange>()
        val unmatched = mutableListOf<UnmatchedEmail>()

        for (msg in newMessages) {
            val msgId = msg["id"]?.toString() ?: msg["messageId"]?.toString() ?: ""
            val sender = msg["sender"]?.toString() ?: msg["from"]?.toString()
            val subject = msg["subject"]?.toString()
            val snippet = msg["snippet"]?.toString()
            val receivedAt = msg["receivedAt"]?.toString()

            val sourceEmail = SourceEmail(
                messageId = msgId,
                subject = subject,
                sender = sender,
                receivedAt = receivedAt,
                snippet = snippet
            )

            // Filter out 2FA / OTP verification noise from generating proposals
            val isVerificationNoise = Regex(
                "(?i)(?:verification\\s+code|confirm\\s+your\\s+identity|one-time\\s+passcode|\\botp\\b|security\\s+code|password\\s+reset)"
            ).containsMatchIn("${subject.orEmpty()} ${snippet.orEmpty()}")
            if (isVerificationNoise) {
                continue
            }

            // Match against tracked applications
            val matchResult = EmailClassifier.match(msg, applications)
            if (matchResult.applicationId != null) {
                val appId = matchResult.applicationId!!
                val app = applications[appId] ?: emptyMap()
                val currentStatus = app["status"]?.toString() ?: "UNKNOWN"

                val classified = EmailClassifier.classify(subject, snippet)
                val eventType = classified?.eventType ?: "RECEIVED"
                val statusTo = classified?.statusTo
                val confidence = classified?.confidence ?: "LOW_CONFIDENCE"

                changes.add(
                    StagedEmailChange(
                        applicationId = appId,
                        eventType = eventType,
                        statusFrom = currentStatus,
                        statusTo = statusTo,
                        confidence = confidence,
                        sourceEmail = sourceEmail
                    )
                )
            } else {
                // Check if it's an outreach lead from a company
                val leadCompany = EmailClassifier.extractCompany(msg)
                if (leadCompany != null) {
                    changes.add(
                        StagedEmailChange(
                            applicationId = null,
                            eventType = "LEAD",
                            statusFrom = null,
                            statusTo = "CONSIDERING",
                            confidence = "MEDIUM_CONFIDENCE",
                            sourceEmail = sourceEmail,
                            leadCompany = leadCompany,
                            leadChannel = "EMAIL"
                        )
                    )
                } else {
                    unmatched.add(
                        UnmatchedEmail(
                            sender = sender,
                            subject = subject,
                            receivedAt = receivedAt,
                            reason = matchResult.reason ?: "Unmatched company and non-lead",
                            snippet = snippet
                        )
                    )
                }
            }
        }

        t.println("Classified: ${changes.size} staged change(s), ${unmatched.size} unmatched message(s)")

        if (dryRun) {
            t.println(green("✓ [dry-run] Proposal preview complete."))
            return
        }

        val proposalId = UUID.randomUUID().toString()
        val nowIso = Instant.now().toString()
        val proposalDoc = mapOf(
            "proposalId" to proposalId,
            "state" to "PENDING",
            "trigger" to (trigger ?: "manual"),
            "createdAt" to nowIso,
            "collectedAt" to nowIso,
            "changes" to changes.map { c ->
                mapOf(
                    "applicationId" to c.applicationId,
                    "eventType" to c.eventType,
                    "statusFrom" to c.statusFrom,
                    "statusTo" to c.statusTo,
                    "confidence" to c.confidence,
                    "leadCompany" to c.leadCompany,
                    "leadChannel" to c.leadChannel,
                    "sourceEmail" to mapOf(
                        "messageId" to c.sourceEmail.messageId,
                        "subject" to c.sourceEmail.subject,
                        "sender" to c.sourceEmail.sender,
                        "receivedAt" to c.sourceEmail.receivedAt,
                        "snippet" to c.sourceEmail.snippet
                    )
                )
            },
            "unmatched" to unmatched.map { u ->
                mapOf(
                    "sender" to u.sender,
                    "subject" to u.subject,
                    "receivedAt" to u.receivedAt,
                    "reason" to u.reason,
                    "snippet" to u.snippet
                )
            }
        )

        db.collection("pending-proposals").document(proposalId).set(proposalDoc).get()

        // Update processed message IDs in meta/sync
        val newIds = newMessages.mapNotNull { it["id"]?.toString() ?: it["messageId"]?.toString() }
        db.collection("meta").document("sync").set(
            mapOf(
                "gmail" to mapOf(
                    "processedMessageIds" to FieldValue.arrayUnion(*newIds.toTypedArray()),
                    "lastCollectedAt" to nowIso
                )
            ),
            SetOptions.merge()
        ).get()

        t.println(green("✓ Staged proposal $proposalId (${changes.size} changes, ${unmatched.size} unmatched)"))
    }
}

internal class EmailDiscoverCommand : CliktCommand(name = "discover") {
    private val all: Boolean by option("--all", help = "Scan all cached emails").flag(default = false)
    private val since: String? by option("--since", help = "Only scan emails since this date (YYYY-MM-DD)")
    private val dryRun: Boolean by option("--dry-run", help = "Preview without writing").flag(default = false)

    override fun run() {
        val t = Terminal()
        t.println(cyan("Scanning cached emails for untracked applications (discovery)..."))

        if (!dryRun) {
            RemoteDataSourceProvider.assertWritesAllowed("email discovery staging")
        }

        val candidates = BackendService.discoverUntrackedApplications(
            inboxFile = File(getGeneratedDir(), "gmail-inbox.json").takeIf { it.exists() },
            since = since
        )

        t.println("Found ${candidates.size} untracked discovery candidate(s)")

        if (dryRun) {
            candidates.forEach { c ->
                t.println("  • Discovered: ${c.company} (${c.role})")
            }
            t.println(green("✓ [dry-run] Discovery complete."))
            return
        }

        if (candidates.isNotEmpty()) {
            val result = BackendService.stageDiscoveryProposals(candidates)
            t.println(green("✓ Successfully staged discovery proposals: $result"))
        } else {
            t.println(green("No untracked applications discovered."))
        }
    }
}


private fun findProjectRoot(): File {
    var dir = File(".").canonicalFile
    while (dir.parentFile != null && !File(dir, "mise.toml").exists()) {
        dir = dir.parentFile
    }
    return dir
}

private fun getGeneratedDir(): File {
    val dir = File(findProjectRoot(), "generated")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

private fun findComposioBin(): String? {
    val home = System.getProperty("user.home")
    val defaultPath = "$home/.local/bin/composio"
    if (File(defaultPath).canExecute()) return defaultPath

    val pathDirs = System.getenv("PATH")?.split(":") ?: emptyList()
    for (dir in pathDirs) {
        val bin = File(dir, "composio")
        if (bin.canExecute()) return bin.absolutePath
    }
    return null
}

private fun extractJsonFromOutput(output: String): String {
    val trimmed = output.trim()
    val startBrace = trimmed.indexOf('{')
    val startBracket = trimmed.indexOf('[')
    val startIndex = when {
        startBrace != -1 && startBracket != -1 -> minOf(startBrace, startBracket)
        startBrace != -1 -> startBrace
        startBracket != -1 -> startBracket
        else -> -1
    }
    if (startIndex == -1) return "{}"

    val endBrace = trimmed.lastIndexOf('}')
    val endBracket = trimmed.lastIndexOf(']')
    val endIndex = maxOf(endBrace, endBracket)
    if (endIndex == -1 || endIndex < startIndex) return "{}"

    val jsonText = trimmed.substring(startIndex, endIndex + 1)
    try {
        val parsed = Json.parseToJsonElement(jsonText)
        if (parsed is JsonObject) {
            val storedInFile = parsed["storedInFile"]?.jsonPrimitive?.booleanOrNull ?: false
            if (storedInFile) {
                val filePath = parsed["outputFilePath"]?.jsonPrimitive?.contentOrNull
                if (filePath != null && File(filePath).exists()) {
                    return File(filePath).readText()
                }
            }
        }
    } catch (_: Exception) {}

    return jsonText
}
