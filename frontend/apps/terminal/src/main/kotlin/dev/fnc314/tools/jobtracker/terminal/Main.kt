package dev.fnc314.tools.jobtracker.terminal

import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Panel
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import dev.fnc314.tools.jobtracker.core.data.platform.RemotePlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.Scanner

public data class ApplicationRecord(
    val id: String,
    val company: String,
    val role: String,
    val status: String,
    val fitRating: String,
    val isOpen: Boolean,
    val channel: String,
    val location: String,
    val jobUrl: String,
    val jobApplicationUrl: String,
    val uuid: String,
    val emailMessageIds: List<String>,
    val tags: List<String>,
    val date: String,
    val rawData: Map<String, Any?> = emptyMap()
)

public data class TimelineEventRecord(
    val ts: String,
    val type: String,
    val actor: String,
    val emailMessageId: String,
    val note: String
)

public data class ProposalRecord(
    val proposalId: String,
    val createdAt: String,
    val state: String,
    val changesCount: Int,
    val unmatchedCount: Int,
    var enabled: Boolean = true,
    val changes: List<Map<String, Any?>> = emptyList(),
    val unmatched: List<Map<String, Any?>> = emptyList()
)

public object TerminalApp {
    private val t: Terminal = Terminal()
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; isLenient = true }
    public var isOffline: Boolean = false
        private set

    private val repoRoot: File by lazy { findRepoRoot() }

    private val firestore: Firestore? by lazy {
        if (System.getenv("TRACKER_SOURCE")?.lowercase() == "cache") {
            isOffline = true
            null
        } else {
            try {
                RemotePlatform.initialize()
                val app = FirebaseApp.getInstance()
                FirestoreClient.getFirestore(app)
            } catch (e: Exception) {
                isOffline = true
                null
            }
        }
    }

    private fun findRepoRoot(): File {
        var cur: File? = File(".").canonicalFile
        while (cur != null) {
            if (File(cur, "mise.toml").exists() || File(cur, "settings.gradle.kts").exists()) {
                return cur
            }
            cur = cur.parentFile
        }
        return File(".").canonicalFile
    }

    private fun findCacheFile(): File? {
        val candidates = listOf(
            File(repoRoot, "generated/tracker-cache.json"),
            File("generated/tracker-cache.json"),
            File("../generated/tracker-cache.json"),
            File("../../generated/tracker-cache.json"),
            File("../../../generated/tracker-cache.json")
        )
        return candidates.firstOrNull { it.exists() }
    }

    public fun run(args: Array<String>) {
        val argsList = args.toList()

        // 1. Check for proposals review mode
        if (argsList.contains("--proposals") || argsList.firstOrNull() == "proposals") {
            runProposalsMode()
            return
        }

        // 2. Parse flags & environment variables
        var openOnly = System.getenv("TRACKER_FILTER_OPEN") == "1"
        var statusFilter = System.getenv("TRACKER_FILTER_STATUS")
        var tagFilter = System.getenv("TRACKER_FILTER_TAG")
        var limitFilter = System.getenv("TRACKER_FILTER_LIMIT")?.toIntOrNull()
        var forceCache = System.getenv("TRACKER_SOURCE")?.lowercase() == "cache"

        var i = 0
        var singleCommand: String? = null
        var singleArg: String? = null

        while (i < args.size) {
            when (val arg = args[i]) {
                "--open" -> openOnly = true
                "--cache" -> forceCache = true
                "--status" -> {
                    if (i + 1 < args.size) {
                        statusFilter = args[++i]
                    }
                }
                "--tag" -> {
                    if (i + 1 < args.size) {
                        tagFilter = args[++i]
                    }
                }
                "--limit" -> {
                    if (i + 1 < args.size) {
                        limitFilter = args[++i].toIntOrNull()
                    }
                }
                "inspect", "get" -> {
                    singleCommand = "inspect"
                    if (i + 1 < args.size) singleArg = args[++i]
                }
                "open" -> openOnly = true
                "list" -> singleCommand = "list"
                else -> {
                    if (!arg.startsWith("-") && singleCommand == null) {
                        singleCommand = arg
                    }
                }
            }
            i++
        }

        if (forceCache) {
            isOffline = true
        }

        if (singleCommand == "inspect" && singleArg != null) {
            inspectApplication(singleArg)
            return
        }

        if (singleCommand == "list") {
            listApplications(
                openOnly = openOnly,
                statusFilter = statusFilter,
                tagFilter = tagFilter,
                limit = limitFilter
            )
            return
        }

        // Interactive pipeline navigation loop
        interactiveLoop(
            initialOpenOnly = openOnly,
            initialStatusFilter = statusFilter,
            initialTagFilter = tagFilter,
            initialLimit = limitFilter
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Cache & Data Loading
    // ─────────────────────────────────────────────────────────────────────────────

    public fun fetchApplications(
        openOnly: Boolean = false,
        statusFilter: String? = null,
        tagFilter: String? = null,
        forceCache: Boolean = false
    ): List<ApplicationRecord> {
        if (!forceCache && !isOffline && firestore != null) {
            try {
                var query = firestore!!.collection("applications") as com.google.cloud.firestore.Query
                if (openOnly) {
                    query = query.whereEqualTo("isOpen", true)
                }
                if (!statusFilter.isNullOrBlank()) {
                    query = query.whereEqualTo("status", normalizeStatusKey(statusFilter))
                }
                val snapshot = query.get().get()
                return snapshot.documents.map { doc ->
                    val data = doc.data
                    val locMap = data["location"] as? Map<*, *>
                    val locStr = listOfNotNull(locMap?.get("city"), locMap?.get("region"), locMap?.get("country"))
                        .joinToString(", ").ifEmpty { data["location"]?.toString() ?: "-" }

                    val emailList = (data["emailMessageIds"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val tagList = (data["tags"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                    ApplicationRecord(
                        id = doc.id,
                        company = data["company"]?.toString() ?: "-",
                        role = data["role"]?.toString() ?: "-",
                        status = cleanStatus(data["status"]?.toString() ?: "-"),
                        fitRating = data["fitRating"]?.toString() ?: "-",
                        isOpen = data["isOpen"] as? Boolean ?: true,
                        channel = cleanChannel(data["channel"]?.toString() ?: "-"),
                        location = locStr,
                        jobUrl = data["jobUrl"]?.toString() ?: data["source"]?.toString() ?: "",
                        jobApplicationUrl = data["jobApplicationUrl"]?.toString() ?: "",
                        uuid = data["uuid"]?.toString() ?: data["uuidv7"]?.toString() ?: "-",
                        emailMessageIds = emailList,
                        tags = tagList,
                        date = data["date"]?.toString() ?: data["createdAt"]?.toString() ?: "",
                        rawData = data
                    )
                }
            } catch (e: Exception) {
                isOffline = true
            }
        }

        // Offline / Cache fallback
        return loadApplicationsFromCache(openOnly, statusFilter, tagFilter)
    }

    public fun loadApplicationsFromCache(
        openOnly: Boolean = false,
        statusFilter: String? = null,
        tagFilter: String? = null
    ): List<ApplicationRecord> {
        isOffline = true
        val cacheFile = findCacheFile() ?: return emptyList()
        return try {
            val rootObj = jsonParser.parseToJsonElement(cacheFile.readText()).jsonObject
            val appsArray = rootObj["applications"]?.jsonArray ?: JsonArray(emptyList())

            appsArray.mapNotNull { el ->
                val obj = el.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val isOpen = obj["isOpen"]?.jsonPrimitive?.booleanOrNull ?: true
                if (openOnly && !isOpen) return@mapNotNull null

                val statusRaw = obj["status"]?.jsonPrimitive?.content ?: "-"
                val cleanSt = cleanStatus(statusRaw)
                if (!statusFilter.isNullOrBlank() && !cleanSt.equals(cleanStatus(statusFilter), ignoreCase = true)) {
                    return@mapNotNull null
                }

                val tagList = obj["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                if (!tagFilter.isNullOrBlank() && !tagList.any { it.contains(tagFilter, ignoreCase = true) }) {
                    return@mapNotNull null
                }

                val locObj = obj["location"] as? JsonObject
                val locStr = listOfNotNull(
                    locObj?.get("city")?.jsonPrimitive?.content,
                    locObj?.get("region")?.jsonPrimitive?.content,
                    locObj?.get("country")?.jsonPrimitive?.content
                ).joinToString(", ").ifEmpty { "-" }

                val emailList = obj["emailMessageIds"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()

                ApplicationRecord(
                    id = id,
                    company = obj["company"]?.jsonPrimitive?.content ?: "-",
                    role = obj["role"]?.jsonPrimitive?.content ?: "-",
                    status = cleanSt,
                    fitRating = obj["fitRating"]?.jsonPrimitive?.content ?: "-",
                    isOpen = isOpen,
                    channel = cleanChannel(obj["channel"]?.jsonPrimitive?.content ?: "-"),
                    location = locStr,
                    jobUrl = obj["jobUrl"]?.jsonPrimitive?.content ?: obj["source"]?.jsonPrimitive?.content ?: "",
                    jobApplicationUrl = obj["jobApplicationUrl"]?.jsonPrimitive?.content ?: "",
                    uuid = obj["uuid"]?.jsonPrimitive?.content ?: obj["uuidv7"]?.jsonPrimitive?.content ?: "-",
                    emailMessageIds = emailList,
                    tags = tagList,
                    date = obj["date"]?.jsonPrimitive?.content ?: obj["createdAt"]?.jsonPrimitive?.content ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    public fun loadTimelineEvents(applicationId: String): List<TimelineEventRecord> {
        if (!isOffline && firestore != null) {
            try {
                val docRef = firestore!!.collection("applications").document(applicationId)
                val eventsSnap = docRef.collection("events").orderBy("ts").get().get()
                return eventsSnap.documents.map { evDoc ->
                    val ev = evDoc.data
                    TimelineEventRecord(
                        ts = ev["ts"]?.toString() ?: "",
                        type = cleanType(ev["eventType"]?.toString() ?: ev["type"]?.toString() ?: "-"),
                        actor = cleanActor(ev["actor"]?.toString() ?: "-"),
                        emailMessageId = ev["emailMessageId"]?.toString() ?: "-",
                        note = ev["note"]?.toString() ?: ""
                    )
                }
            } catch (e: Exception) {
                isOffline = true
            }
        }

        // Cache fallback for events
        val cacheFile = findCacheFile() ?: return emptyList()
        return try {
            val rootObj = jsonParser.parseToJsonElement(cacheFile.readText()).jsonObject
            val eventsMap = rootObj["events"]?.jsonObject ?: return emptyList()
            val appEvents = eventsMap[applicationId]?.jsonArray ?: return emptyList()
            appEvents.map { el ->
                val obj = el.jsonObject
                TimelineEventRecord(
                    ts = obj["ts"]?.jsonPrimitive?.content ?: "",
                    type = cleanType(obj["type"]?.jsonPrimitive?.content ?: "-"),
                    actor = cleanActor(obj["actor"]?.jsonPrimitive?.content ?: "-"),
                    emailMessageId = obj["emailMessageId"]?.jsonPrimitive?.content ?: "-",
                    note = obj["note"]?.jsonPrimitive?.content ?: ""
                )
            }.sortedBy { it.ts }
        } catch (e: Exception) {
            emptyList()
        }
    }

    public fun loadPendingProposals(): List<ProposalRecord> {
        // Query proposals from Firestore if online
        if (!isOffline && firestore != null) {
            try {
                val snapshot = firestore!!.collection("pending-proposals")
                    .whereIn("state", listOf("proposal:pending", "PENDING"))
                    .get().get()
                if (!snapshot.isEmpty) {
                    return snapshot.documents.map { doc ->
                        val data = doc.data
                        val changes = (data["changes"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
                        val unmatched = (data["unmatched"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
                        ProposalRecord(
                            proposalId = data["proposalId"]?.toString() ?: doc.id,
                            createdAt = data["createdAt"]?.toString() ?: "",
                            state = data["state"]?.toString() ?: "PENDING",
                            changesCount = changes.size,
                            unmatchedCount = unmatched.size,
                            changes = changes,
                            unmatched = unmatched
                        )
                    }.sortedByDescending { it.createdAt }
                }
            } catch (e: Exception) {
                isOffline = true
            }
        }

        // Fallback: try tracker.py CLI listing if available
        try {
            val trackerScript = File(repoRoot, "scripts/tracker.py")
            if (trackerScript.exists()) {
                val pb = ProcessBuilder("python3", trackerScript.absolutePath, "proposals", "list", "--state", "proposal:pending")
                pb.directory(repoRoot)
                val proc = pb.start()
                val stdout = proc.inputStream.bufferedReader().readText()
                if (proc.waitFor() == 0 && stdout.isNotBlank()) {
                    val root = jsonParser.parseToJsonElement(stdout).jsonObject
                    val list = root["proposals"]?.jsonArray
                    if (list != null) {
                        return list.map { el ->
                            val obj = el.jsonObject
                            val changesList = obj["changes"]?.jsonArray ?: JsonArray(emptyList())
                            val unmatchedList = obj["unmatched"]?.jsonArray ?: JsonArray(emptyList())
                            ProposalRecord(
                                proposalId = obj["proposalId"]?.jsonPrimitive?.content ?: "",
                                createdAt = obj["createdAt"]?.jsonPrimitive?.content ?: "",
                                state = obj["state"]?.jsonPrimitive?.content ?: "PENDING",
                                changesCount = changesList.size,
                                unmatchedCount = unmatchedList.size
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore CLI fallback error
        }

        // Cache fallback
        val cacheFile = findCacheFile() ?: return emptyList()
        return try {
            val rootObj = jsonParser.parseToJsonElement(cacheFile.readText()).jsonObject
            val pendingArray = rootObj["pendingProposals"]?.jsonArray ?: return emptyList()
            pendingArray.map { el ->
                val obj = el.jsonObject
                val changesCount = obj["changes"]?.jsonPrimitive?.intOrNull ?: (obj["changes"] as? JsonArray)?.size ?: 0
                val unmatchedCount = obj["unmatched"]?.jsonPrimitive?.intOrNull ?: (obj["unmatched"] as? JsonArray)?.size ?: 0
                ProposalRecord(
                    proposalId = obj["proposalId"]?.jsonPrimitive?.content ?: "",
                    createdAt = obj["createdAt"]?.jsonPrimitive?.content ?: "",
                    state = "PENDING",
                    changesCount = changesCount,
                    unmatchedCount = unmatchedCount
                )
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Interactive Proposals Review Mode (--proposals)
    // ─────────────────────────────────────────────────────────────────────────────

    public fun runProposalsMode() {
        val proposals = loadPendingProposals().toMutableList()

        if (proposals.isEmpty()) {
            System.err.println("No pending proposals to review.")
            // Machine-readable contract output on stdout
            println("""{"ok": true, "remaining_pending": 0, "summary": "proposals review complete"}""")
            return
        }

        System.err.println()
        System.err.println(t.render(
            Panel(
                content = bold(brightBlue("AI Job Search Tracker — Interactive Proposals Review")) +
                    "\n" + dim("Review staged inbox proposals • Press Space to toggle selection, [a] approve, [r] reject, [q] quit"),
                title = cyan("★ Staged Proposals Review ★")
            )
        ))

        fun renderProposalsList() {
            val enabledCount = proposals.count { it.enabled }
            System.err.println()
            System.err.println(
                cyan("PROPOSALS REVIEW") + white(" | ") +
                    bold("${proposals.size} pending") + white(" | ") +
                    green("$enabledCount enabled for batch action")
            )
            System.err.println()

            val tbl = table {
                header {
                    row(cyan("Sel"), cyan("Proposal ID"), cyan("Created Date"), cyan("Changes"), cyan("Unmatched"))
                }
                body {
                    proposals.forEachIndexed { idx, p ->
                        val check = if (p.enabled) green("[x]") else dim("[ ]")
                        val dateFormatted = p.createdAt.take(19).replace("T", " ")
                        val changesFormatted = if (p.changesCount > 0) green(p.changesCount.toString()) else dim("0")
                        val unmatchedFormatted = if (p.unmatchedCount > 0) yellow(p.unmatchedCount.toString()) else dim("0")
                        row(check, p.proposalId, dateFormatted, changesFormatted, unmatchedFormatted)
                    }
                }
            }
            System.err.println(t.render(tbl))
        }

        renderProposalsList()

        val scanner = Scanner(System.`in`)
        while (true) {
            System.err.println()
            System.err.print(t.render(brightBlue("proposals [a:approve, r:reject, t <id>:toggle, i <id>:inspect, q:quit] > ")))
            if (!scanner.hasNextLine()) break
            val line = scanner.nextLine().trim()
            if (line.isEmpty()) continue

            val parts = line.split("\\s+".toRegex())
            val cmd = parts[0].lowercase()

            when (cmd) {
                "q", "quit", "exit" -> break

                "t", "toggle" -> {
                    if (parts.size > 1) {
                        val targetId = parts[1]
                        val prop = proposals.find { it.proposalId.startsWith(targetId) }
                        if (prop != null) {
                            prop.enabled = !prop.enabled
                            renderProposalsList()
                        } else {
                            System.err.println(red("Proposal not found: $targetId"))
                        }
                    } else {
                        // Toggle first or prompt
                        System.err.println(yellow("Usage: t <id-prefix> to toggle selection"))
                    }
                }

                "i", "inspect" -> {
                    if (parts.size > 1) {
                        inspectProposal(parts[1])
                    } else {
                        System.err.println(yellow("Usage: i <id-prefix> to inspect details"))
                    }
                }

                "a", "approve" -> {
                    val toApprove = if (parts.size > 1) {
                        proposals.filter { it.proposalId.startsWith(parts[1]) }
                    } else {
                        proposals.filter { it.enabled }
                    }

                    if (toApprove.isEmpty()) {
                        System.err.println(yellow("No proposals selected for approval."))
                        continue
                    }

                    var approvedCount = 0
                    var failCount = 0
                    val completedIds = mutableListOf<String>()

                    for (p in toApprove) {
                        val (ok, msg) = runProposalAction("approve", p.proposalId)
                        if (ok) {
                            approvedCount++
                            completedIds.add(p.proposalId)
                            System.err.println(green("✔ Approved: ${p.proposalId}"))
                        } else {
                            failCount++
                            System.err.println(red("✘ Failed ${p.proposalId}: $msg"))
                        }
                    }

                    proposals.removeAll { it.proposalId in completedIds }
                    System.err.println(bold("Batch complete: $approvedCount approved, $failCount failed."))
                    if (proposals.isEmpty()) {
                        System.err.println(green("All proposals processed! Exiting."))
                        break
                    }
                    renderProposalsList()
                }

                "r", "reject" -> {
                    val toReject = if (parts.size > 1) {
                        proposals.filter { it.proposalId.startsWith(parts[1]) }
                    } else {
                        proposals.filter { it.enabled }
                    }

                    if (toReject.isEmpty()) {
                        System.err.println(yellow("No proposals selected for rejection."))
                        continue
                    }

                    var rejectedCount = 0
                    var failCount = 0
                    val completedIds = mutableListOf<String>()

                    for (p in toReject) {
                        val (ok, msg) = runProposalAction("reject", p.proposalId)
                        if (ok) {
                            rejectedCount++
                            completedIds.add(p.proposalId)
                            System.err.println(yellow("✔ Rejected: ${p.proposalId}"))
                        } else {
                            failCount++
                            System.err.println(red("✘ Failed ${p.proposalId}: $msg"))
                        }
                    }

                    proposals.removeAll { it.proposalId in completedIds }
                    System.err.println(bold("Batch complete: $rejectedCount rejected, $failCount failed."))
                    if (proposals.isEmpty()) {
                        System.err.println(green("All proposals processed! Exiting."))
                        break
                    }
                    renderProposalsList()
                }

                else -> {
                    System.err.println(red("Unknown command '$cmd'. Commands: 'a' (approve), 'r' (reject), 't <id>' (toggle), 'i <id>' (inspect), 'q' (quit)."))
                }
            }
        }

        // Emit final summary JSON on stdout for tracker.py
        val remaining = proposals.size
        println("""{"ok": true, "remaining_pending": $remaining, "summary": "proposals review complete"}""")
    }

    private fun inspectProposal(proposalIdPrefix: String) {
        val trackerScript = File(repoRoot, "scripts/tracker.py")
        if (!trackerScript.exists()) {
            System.err.println(red("tracker.py not found at ${trackerScript.absolutePath}"))
            return
        }

        try {
            val pb = ProcessBuilder("python3", trackerScript.absolutePath, "proposals", "show", proposalIdPrefix)
            pb.directory(repoRoot)
            val proc = pb.start()
            val stdout = proc.inputStream.bufferedReader().readText()
            if (proc.waitFor() == 0 && stdout.isNotBlank()) {
                val root = jsonParser.parseToJsonElement(stdout).jsonObject
                val prop = root["proposal"]?.jsonObject
                if (prop != null) {
                    val pid = prop["proposalId"]?.jsonPrimitive?.content ?: proposalIdPrefix
                    val created = prop["createdAt"]?.jsonPrimitive?.content ?: "-"
                    val trigger = prop["trigger"]?.jsonPrimitive?.content ?: "-"
                    val changes = prop["changes"]?.jsonArray ?: JsonArray(emptyList())
                    val unmatched = prop["unmatched"]?.jsonArray ?: JsonArray(emptyList())

                    val content = buildString {
                        appendLine("Proposal ID:  ${bold(pid)}")
                        appendLine("Created At:   $created")
                        appendLine("Trigger:      $trigger")
                        appendLine("Changes:      ${changes.size}")
                        appendLine("Unmatched:    ${unmatched.size}")
                        if (changes.isNotEmpty()) {
                            appendLine()
                            appendLine(bold("=== Proposed State Changes (${changes.size}) ==="))
                            changes.forEachIndexed { idx, chEl ->
                                val ch = chEl.jsonObject
                                val appTarget = ch["applicationId"]?.jsonPrimitive?.content ?: "-"
                                val stFrom = cleanStatus(ch["statusFrom"]?.jsonPrimitive?.content ?: "")
                                val stTo = cleanStatus(ch["statusTo"]?.jsonPrimitive?.content ?: "")
                                val conf = ch["confidence"]?.jsonPrimitive?.content ?: "medium"
                                val srcEmail = ch["sourceEmail"]?.jsonObject
                                val subj = srcEmail?.get("subject")?.jsonPrimitive?.content ?: "(no subject)"
                                val trans = if (stFrom.isNotBlank()) "$stFrom → $stTo" else "→ $stTo"
                                appendLine("  ${idx + 1}. [$appTarget] $trans (Confidence: $conf)")
                                appendLine("     Email: $subj")
                            }
                        }
                        if (unmatched.isNotEmpty()) {
                            appendLine()
                            appendLine(bold("=== Unmatched Emails (${unmatched.size}) ==="))
                            unmatched.take(10).forEachIndexed { idx, uEl ->
                                val u = uEl.jsonObject
                                val subj = u["subject"]?.jsonPrimitive?.content ?: "(no subject)"
                                val reason = u["reason"]?.jsonPrimitive?.content ?: "-"
                                appendLine("  ${idx + 1}. $subj")
                                appendLine("     Reason: $reason")
                            }
                            if (unmatched.size > 10) {
                                appendLine("  ... and ${unmatched.size - 10} more unmatched items.")
                            }
                        }
                    }

                    System.err.println(t.render(
                        Panel(
                            content = content,
                            title = cyan("Proposal Details: $pid")
                        )
                    ))
                    return
                }
            }
        } catch (e: Exception) {
            System.err.println(red("Failed to inspect proposal: ${e.message}"))
        }
    }

    private fun runProposalAction(action: String, proposalId: String, note: String? = null): Pair<Boolean, String> {
        val trackerScript = File(repoRoot, "scripts/tracker.py")
        if (!trackerScript.exists()) {
            return false to "tracker.py not found"
        }
        val cmd = mutableListOf(
            "python3",
            trackerScript.absolutePath,
            "proposals",
            action,
            proposalId
        )
        if (!note.isNullOrBlank()) {
            cmd.add("--note")
            cmd.add(note)
        }
        return try {
            val pb = ProcessBuilder(cmd)
            pb.directory(repoRoot)
            pb.environment()["TRACKER_ALLOW_WRITES"] = "1"
            val proc = pb.start()
            val stdout = proc.inputStream.bufferedReader().readText()
            val stderr = proc.errorStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            if (exitCode == 0) {
                true to "${action.replaceFirstChar { it.uppercase() }}d $proposalId"
            } else {
                val err = stderr.trim().ifEmpty { stdout.trim() }.ifEmpty { "Failed to $action" }
                false to err
            }
        } catch (e: Exception) {
            false to (e.message ?: "Execution error")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pipeline Navigation & Inspection
    // ─────────────────────────────────────────────────────────────────────────────

    private fun renderHeader() {
        t.println()
        val sourceBadge = if (isOffline) yellow(" [SOURCE: CACHE (OFFLINE)]") else ""
        val banner = Panel(
            content = bold(brightBlue("AI Job Search Tracker — Kotlin Terminal Interface$sourceBadge")) +
                "\n" + dim("Monorepo Architecture (records + backend + frontend) • Scale-to-Zero Cloud Run"),
            title = cyan("★ Job Application Pipeline ★")
        )
        t.println(banner)
    }

    public fun listApplications(
        openOnly: Boolean = false,
        filterQuery: String? = null,
        statusFilter: String? = null,
        tagFilter: String? = null,
        limit: Int? = null,
        sortCol: String? = null
    ) {
        renderHeader()
        if (!isOffline) {
            t.println(dim("Connecting to pipeline storage..."))
        }

        var docs = fetchApplications(openOnly = openOnly, statusFilter = statusFilter, tagFilter = tagFilter)

        if (!filterQuery.isNullOrBlank()) {
            val q = filterQuery.lowercase()
            docs = docs.filter { doc ->
                doc.id.lowercase().contains(q) ||
                    doc.company.lowercase().contains(q) ||
                    doc.role.lowercase().contains(q) ||
                    doc.tags.any { it.lowercase().contains(q) }
            }
        }

        docs = when (sortCol?.lowercase()) {
            "company" -> docs.sortedBy { it.company }
            "role" -> docs.sortedBy { it.role }
            "status" -> docs.sortedBy { it.status }
            "fit" -> docs.sortedByDescending { it.fitRating.toIntOrNull() ?: -1 }
            "date" -> docs.sortedByDescending { it.date }
            else -> docs.sortedBy { it.id }
        }

        if (limit != null && limit > 0) {
            docs = docs.take(limit)
        }

        val totalCount = docs.size
        val openCount = docs.count { it.isOpen }
        val interviewCount = docs.count { it.status.uppercase() == "INTERVIEW" }
        val offerCount = docs.count { it.status.uppercase() == "OFFER" }
        val leadCount = docs.count { it.status.uppercase() == "LEAD" }

        t.println(
            white("Filter: ") + (if (openOnly) green("OPEN ONLY") else cyan("ALL")) +
                white(" | Total: ") + bold(totalCount.toString()) +
                white(" | Open: ") + green(openCount.toString()) +
                white(" | Interviews: ") + yellow(interviewCount.toString()) +
                white(" | Offers: ") + bold(green(offerCount.toString())) +
                white(" | Leads: ") + magenta(leadCount.toString())
        )
        t.println()

        if (docs.isEmpty()) {
            t.println(gray("No applications matched the active criteria."))
            return
        }

        val tbl = table {
            header {
                row(cyan("ID"), cyan("Company"), cyan("Role"), cyan("Status"), cyan("Fit"), cyan("Sync Cues"), cyan("Emails"))
            }
            body {
                docs.forEach { doc ->
                    val statusFormatted = when (doc.status.uppercase()) {
                        "APPLIED" -> green(doc.status)
                        "INTERVIEW" -> yellow(doc.status)
                        "OFFER", "HIRED" -> bold(green(doc.status))
                        "REJECTED" -> red(doc.status)
                        "WITHDRAWN" -> dim(doc.status)
                        "LEAD" -> magenta(doc.status)
                        else -> doc.status
                    }

                    val syncCue = if (doc.emailMessageIds.isNotEmpty()) {
                        green("[AUTO-SYNCED]")
                    } else {
                        dim("-")
                    }

                    val emailsBadge = if (doc.emailMessageIds.isNotEmpty()) {
                        cyan("${doc.emailMessageIds.size} linked")
                    } else {
                        dim("0")
                    }

                    row(doc.id, doc.company, doc.role, statusFormatted, doc.fitRating, syncCue, emailsBadge)
                }
            }
        }
        t.println(tbl)
    }

    public fun inspectApplication(applicationId: String) {
        val app = fetchApplications().find { it.id.equals(applicationId, ignoreCase = true) }
        if (app == null) {
            t.println(red("Application not found: $applicationId"))
            return
        }

        val detailsContent = buildString {
            appendLine("Company:       ${bold(app.company)}")
            appendLine("Role:          ${bold(app.role)}")
            appendLine("Status:        ${app.status}")
            appendLine("Channel:       ${app.channel}")
            appendLine("Location:      ${app.location}")
            appendLine("Fit Rating:    ${app.fitRating}")
            appendLine("UUID:          ${app.uuid}")
            appendLine("Job URL:       ${app.jobUrl.ifEmpty { "-" }}")
            appendLine("Portal URL:    ${app.jobApplicationUrl.ifEmpty { "-" }}")
            if (app.tags.isNotEmpty()) {
                appendLine("Tags:          ${app.tags.joinToString(", ")}")
            }
        }

        t.println(
            Panel(
                content = detailsContent,
                title = cyan("Application: ${app.id}")
            )
        )

        // Related Emails Card
        val emailCardContent = if (app.emailMessageIds.isEmpty()) {
            gray("No linked email messages recorded.")
        } else {
            app.emailMessageIds.joinToString("\n") { "  • ${green(it)} ${dim("(Linked)")}" }
        }

        t.println(
            Panel(
                content = emailCardContent,
                title = green("✉ Related Emails (${app.emailMessageIds.size})")
            )
        )

        // Timeline Events Card
        val events = loadTimelineEvents(app.id)
        t.println()
        t.println(cyan("=== Timeline History (${events.size} events) ==="))
        if (events.isEmpty()) {
            t.println(gray("No timeline events recorded."))
        } else {
            val eventsTbl = table {
                header {
                    row(cyan("Timestamp"), cyan("Type"), cyan("Actor"), cyan("Email Msg ID"), cyan("Notes"))
                }
                body {
                    events.forEach { ev ->
                        row(ev.ts.take(19).replace("T", " "), ev.type, ev.actor, ev.emailMessageId, ev.note)
                    }
                }
            }
            t.println(eventsTbl)
        }
    }

    public fun openBrowserUrl(applicationId: String): Boolean {
        val app = fetchApplications().find { it.id.equals(applicationId, ignoreCase = true) }
        if (app == null) {
            t.println(red("Application not found: $applicationId"))
            return false
        }
        val targetUrl = app.jobUrl.ifEmpty { app.jobApplicationUrl }
        if (targetUrl.isBlank()) {
            t.println(yellow("No URL recorded for application: $applicationId"))
            return false
        }

        t.println(cyan("Opening browser: $targetUrl"))
        return try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(targetUrl))
                true
            } else {
                val os = System.getProperty("os.name").lowercase()
                val cmd = if (os.contains("mac")) listOf("open", targetUrl) else listOf("xdg-open", targetUrl)
                ProcessBuilder(cmd).start()
                true
            }
        } catch (e: Exception) {
            t.println(red("Failed to open browser: ${e.message}"))
            false
        }
    }

    public fun queueRecalculation(applicationId: String): Boolean {
        val app = fetchApplications().find { it.id.equals(applicationId, ignoreCase = true) }
        if (app == null) {
            t.println(red("Application not found: $applicationId"))
            return false
        }

        val queueFile = File(repoRoot, ".recalc_queue")
        val nowIso = java.time.Instant.now().toString()
        val requestJson = """
        {
          "schemaVersion": 4,
          "kind": "fit-recalculation",
          "requestedAt": "$nowIso",
          "applicationId": "${app.id}",
          "company": "${app.company}",
          "role": "${app.role}",
          "currentFitRating": "${app.fitRating}"
        }
        """.trimIndent()

        return try {
            queueFile.writeText(requestJson + "\n")
            t.println(green("✔ Queued fit recalculation for ${app.id} to .recalc_queue"))
            true
        } catch (e: Exception) {
            t.println(red("Failed to write .recalc_queue: ${e.message}"))
            false
        }
    }

    private fun interactiveLoop(
        initialOpenOnly: Boolean,
        initialStatusFilter: String?,
        initialTagFilter: String?,
        initialLimit: Int?
    ) {
        var openOnly = initialOpenOnly
        var statusFilter = initialStatusFilter
        var tagFilter = initialTagFilter
        var limit = initialLimit
        var filterQuery: String? = null
        var sortCol: String? = null

        listApplications(
            openOnly = openOnly,
            filterQuery = filterQuery,
            statusFilter = statusFilter,
            tagFilter = tagFilter,
            limit = limit,
            sortCol = sortCol
        )

        val scanner = Scanner(System.`in`)
        while (true) {
            t.println()
            t.print(brightBlue("tracker [o:open, l:leads, s <status>, / <query>, i <id>, b <id>, r <id>, q:quit] > "))
            if (!scanner.hasNextLine()) break
            val line = scanner.nextLine().trim()
            if (line.isEmpty()) continue

            val parts = line.split("\\s+".toRegex())
            val cmd = parts[0].lowercase()

            when (cmd) {
                "q", "quit", "exit" -> {
                    t.println(cyan("Exiting tracker. Goodbye!"))
                    break
                }
                "o", "open" -> {
                    openOnly = !openOnly
                    listApplications(openOnly, filterQuery, statusFilter, tagFilter, limit, sortCol)
                }
                "lead", "leads", "l" -> {
                    statusFilter = if (statusFilter == "LEAD") null else "LEAD"
                    listApplications(openOnly, filterQuery, statusFilter, tagFilter, limit, sortCol)
                }
                "s", "status" -> {
                    statusFilter = if (parts.size > 1) parts[1] else null
                    listApplications(openOnly, filterQuery, statusFilter, tagFilter, limit, sortCol)
                }
                "/", "f", "filter", "search" -> {
                    filterQuery = if (parts.size > 1) parts.drop(1).joinToString(" ") else null
                    listApplications(openOnly, filterQuery, statusFilter, tagFilter, limit, sortCol)
                }
                "sort" -> {
                    sortCol = if (parts.size > 1) parts[1] else null
                    listApplications(openOnly, filterQuery, statusFilter, tagFilter, limit, sortCol)
                }
                "all", "reset" -> {
                    openOnly = false
                    statusFilter = null
                    tagFilter = null
                    filterQuery = null
                    sortCol = null
                    listApplications(openOnly, filterQuery, statusFilter, tagFilter, limit, sortCol)
                }
                "i", "inspect", "get" -> {
                    if (parts.size > 1) {
                        inspectApplication(parts[1])
                    } else {
                        t.println(yellow("Usage: i <applicationId>"))
                    }
                }
                "b", "browse", "open-url" -> {
                    if (parts.size > 1) {
                        openBrowserUrl(parts[1])
                    } else {
                        t.println(yellow("Usage: b <applicationId>"))
                    }
                }
                "r", "recalc" -> {
                    if (parts.size > 1) {
                        queueRecalculation(parts[1])
                    } else {
                        t.println(yellow("Usage: r <applicationId>"))
                    }
                }
                "h", "help" -> {
                    t.println(
                        """
                        Available Commands:
                          o, open             - Toggle between Open Only and All
                          l, leads            - Filter leads
                          s <status>          - Filter by status (e.g. 's applied', 's interview')
                          / <query>           - Search in company, role, ID, or tags
                          sort <col>          - Sort by: company, role, status, fit, date, id
                          all, reset          - Clear active filters
                          i <id>              - Deep inspection: details, related emails, and timeline
                          b <id>              - Open Job or Portal URL in default browser
                          r <id>              - Queue fit recalculation to .recalc_queue
                          q, quit             - Exit tracker
                        """.trimIndent()
                    )
                }
                else -> {
                    t.println(red("Unknown command: '$cmd'. Type 'h' for help."))
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // String Normalization Helpers
    // ─────────────────────────────────────────────────────────────────────────────

    public fun cleanStatus(raw: String): String = raw.removePrefix("status:").uppercase()
    public fun cleanChannel(raw: String): String = raw.removePrefix("channel:")
    public fun cleanActor(raw: String): String = raw.removePrefix("actor:")
    public fun cleanType(raw: String): String = raw.removePrefix("event:")

    private fun normalizeStatusKey(status: String): String {
        val s = status.lowercase()
        return if (s.startsWith("status:")) s else "status:$s"
    }
}

public fun main(args: Array<String>) {
    TerminalApp.run(args)
}
