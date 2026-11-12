package dev.fnc314.tools.jobtracker.terminal.tracker

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
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Panel
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import dev.fnc314.tools.jobtracker.core.data.platform.RemotePlatform
import dev.fnc314.tools.jobsearch.parser.InterviewProgress
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
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
    val rawData: Map<String, Any?> = emptyMap(),
    val interviewProgress: InterviewProgress? = null
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
    var enabled: Boolean = false,
    val changes: List<Map<String, Any?>> = emptyList(),
    val unmatched: List<Map<String, Any?>> = emptyList()
)

public data class EmailRecord(
    val id: String,
    val from: String,
    val subject: String,
    val snippet: String,
    val receivedAt: String,
    val status: String,
    val applicationId: String?,
    val companyId: String?
)

// ─────────────────────────────────────────────────────────────────────────────
// Email Rectification Interactive TUI
// ─────────────────────────────────────────────────────────────────────────────

public object EmailsApp {
    private val t: Terminal = Terminal()
    private val scanner = Scanner(System.`in`)
    private val repoRoot: File by lazy {
        var cur: File? = File(".").canonicalFile
        while (cur != null) {
            if (File(cur, "mise.toml").exists() || File(cur, "settings.gradle.kts").exists()) {
                return@lazy cur
            }
            cur = cur.parentFile
        }
        File(".").canonicalFile
    }

    private fun loadEmails(includeNoise: Boolean = false): List<EmailRecord> {
        val firestore = TerminalApp.firestoreInstance
        if (firestore == null) {
            // Fallback to inbox file
            val candidates = listOf(
                File(repoRoot, "generated/gmail-inbox.json"),
                File("generated/gmail-inbox.json"),
                File("../generated/gmail-inbox.json")
            )
            val inboxFile = candidates.firstOrNull { it.exists() } ?: return emptyList()
            val parsed = Json { ignoreUnknownKeys = true }.parseToJsonElement(inboxFile.readText()).jsonObject
            val arr = parsed["messages"]?.jsonArray ?: return emptyList()
            return arr.map {
                val obj = it.jsonObject
                EmailRecord(
                    id = obj["messageId"]?.jsonPrimitive?.content ?: "",
                    from = obj["sender"]?.jsonPrimitive?.content ?: "",
                    subject = obj["subject"]?.jsonPrimitive?.content ?: "",
                    snippet = obj["snippet"]?.jsonPrimitive?.content ?: "",
                    receivedAt = obj["receivedAt"]?.jsonPrimitive?.content ?: "",
                    status = "UNCLASSIFIED",
                    applicationId = null,
                    companyId = null
                )
            }
        }

        val snap = firestore.collection("emails").get().get()
        return snap.documents.mapNotNull { doc ->
            val data = doc.data
            val status = (data["status"] as? String) ?: "UNCLASSIFIED"
            if (!includeNoise && status == "NOISE") return@mapNotNull null
            EmailRecord(
                id = doc.id,
                from = (data["from"] as? String) ?: "",
                subject = (data["subject"] as? String) ?: "",
                snippet = (data["snippet"] as? String) ?: "",
                receivedAt = (data["receivedAt"] as? String) ?: "",
                status = status,
                applicationId = data["applicationId"] as? String,
                companyId = data["companyId"] as? String
            )
        }
    }

    public fun run() {
        var includeNoise = false
        var filterQuery: String? = null
        var statusFilter: String? = null
        var unlinkedOnly = false

        while (true) {
            val allEmails = loadEmails(includeNoise = true)
            val filtered = allEmails.filter { email ->
                if (!includeNoise && email.status == "NOISE") return@filter false
                if (unlinkedOnly && !email.applicationId.isNullOrBlank()) return@filter false
                if (statusFilter != null && !email.status.equals(statusFilter, ignoreCase = true)) return@filter false
                if (filterQuery != null) {
                    val q = filterQuery.lowercase()
                    val matches = email.id.lowercase().contains(q) ||
                        email.from.lowercase().contains(q) ||
                        email.subject.lowercase().contains(q) ||
                        email.snippet.lowercase().contains(q) ||
                        (email.applicationId?.lowercase()?.contains(q) == true)
                    if (!matches) return@filter false
                }
                true
            }

            renderHeader(filtered.size, allEmails.size, includeNoise, unlinkedOnly, statusFilter, filterQuery)
            renderEmailTable(filtered)

            t.println()
            t.print(brightBlue("emails [/ <query>, s <status>, u:unlinked, n:noise, r <id>:rectify, q:quit] > "))
            if (!scanner.hasNextLine()) break
            val line = scanner.nextLine().trim()
            if (line.isEmpty()) continue

            val parts = line.split("\\s+".toRegex())
            when (parts[0].lowercase()) {
                "q", "quit", "exit" -> break
                "n", "noise" -> includeNoise = !includeNoise
                "u", "unlinked" -> unlinkedOnly = !unlinkedOnly
                "/", "f", "search" -> filterQuery = if (parts.size > 1) parts.drop(1).joinToString(" ") else null
                "s", "status" -> statusFilter = if (parts.size > 1) parts[1].uppercase() else null
                "all", "reset" -> {
                    includeNoise = false
                    unlinkedOnly = false
                    filterQuery = null
                    statusFilter = null
                }
                "r", "rectify", "i", "inspect" -> {
                    if (parts.size > 1) {
                        val emailId = parts[1]
                        val email = allEmails.firstOrNull { it.id.equals(emailId, ignoreCase = true) }
                        if (email != null) {
                            handleRectification(email)
                        } else {
                            t.println(red("Email message ID '$emailId' not found."))
                        }
                    } else {
                        t.println(yellow("Usage: r <messageId>"))
                    }
                }
                "h", "help" -> {
                    t.println("""
                        Commands:
                          / <query>     - Search sender, subject, snippet, or app ID
                          s <status>    - Filter by status (e.g. s interview_confirmation, s offer, s noise)
                          u, unlinked   - Toggle unlinked emails only
                          n, noise      - Toggle showing noise emails
                          r <id>        - Inspect & rectify email attribution/status
                          all, reset    - Clear active filters
                          q, quit       - Exit emails view
                    """.trimIndent())
                }
                else -> t.println(red("Unknown command: '${parts[0]}'. Type 'h' for help."))
            }
        }
    }

    private fun renderHeader(
        shown: Int,
        total: Int,
        includeNoise: Boolean,
        unlinkedOnly: Boolean,
        statusFilter: String?,
        filterQuery: String?
    ) {
        t.println()
        val badges = mutableListOf<String>()
        if (unlinkedOnly) badges.add(yellow("[UNLINKED ONLY]"))
        if (includeNoise) badges.add(magenta("[NOISE INCLUDED]"))
        if (statusFilter != null) badges.add(cyan("[STATUS: $statusFilter]"))
        if (filterQuery != null) badges.add(green("[QUERY: '$filterQuery']"))

        t.println(t.render(
            Panel(
                content = bold(brightBlue("AI Job Search Tracker — Email Evidence & Rectification")) + "\n" +
                    dim("Classify evidence, assign applications, filter noise • ") +
                    white("$shown shown") + dim(" of ") + bold("$total total") +
                    if (badges.isNotEmpty()) " " + badges.joinToString(" ") else "",
                title = cyan("★ Email Evidence ★")
            )
        ))
    }

    private fun renderEmailTable(emails: List<EmailRecord>) {
        if (emails.isEmpty()) {
            t.println(gray("No emails match current filter."))
            return
        }

        val tbl = table {
            header {
                row(cyan("Message ID"), cyan("Date"), cyan("Status"), cyan("From"), cyan("Subject"), cyan("Linked App"))
            }
            body {
                emails.take(25).forEach { e ->
                    val statusColor = when (e.status) {
                        "OFFER" -> green
                        "INTERVIEW_REQUEST", "INTERVIEW_CONFIRMATION" -> brightBlue
                        "ASSESSMENT" -> magenta
                        "ACKNOWLEDGEMENT" -> cyan
                        "REJECTION" -> red
                        "NOISE" -> gray
                        else -> yellow
                    }
                    val appDisplay = e.applicationId?.let { bold(cyan(it.take(28))) } ?: dim("unlinked")
                    val dateFormatted = e.receivedAt.take(10)
                    row(
                        dim(e.id.take(12)),
                        dateFormatted,
                        statusColor(e.status.take(18)),
                        e.from.take(22),
                        e.subject.take(34),
                        appDisplay
                    )
                }
            }
        }
        t.println(t.render(tbl))
        if (emails.size > 25) {
            t.println(dim("... and ${emails.size - 25} more (use / <query> or s <status> to narrow)"))
        }
    }

    private fun handleRectification(email: EmailRecord) {
        t.println()
        t.println(cyan("─── EMAIL INSPECTION: ${email.id} ───"))
        t.println("${bold("From:")} ${email.from}")
        t.println("${bold("Date:")} ${email.receivedAt}")
        t.println("${bold("Subject:")} ${bold(email.subject)}")
        t.println("${bold("Status:")} ${cyan(email.status)}")
        t.println("${bold("Linked Application:")} ${email.applicationId ?: dim("unlinked")}")
        t.println("${bold("Snippet:")}\n${dim(email.snippet.take(400))}")
        t.println()

        t.println("""
            Rectification Options:
              [1] Reassign to Application
              [2] Change Status (Acknowledgement, Interview, Assessment, Offer, Rejection, Correspondence)
              [3] Mark as NOISE (unlinks from application)
              [4] Unlink from Application (keep status)
              [b] Cancel / Back
        """.trimIndent())
        t.print(brightBlue("select option [1-4, b] > "))
        if (!scanner.hasNextLine()) return
        val opt = scanner.nextLine().trim()

        when (opt) {
            "1" -> {
                t.print(brightBlue("Enter target applicationId (e.g. pnc-principal-android) > "))
                if (!scanner.hasNextLine()) return
                val targetApp = scanner.nextLine().trim()
                if (targetApp.isNotBlank()) {
                    executeRectify(email.id, targetApp, email.status, isNoise = false)
                }
            }
            "2" -> {
                t.println("""
                    Statuses:
                      [1] ACKNOWLEDGEMENT
                      [2] INTERVIEW_REQUEST
                      [3] INTERVIEW_CONFIRMATION
                      [4] ASSESSMENT
                      [5] OFFER
                      [6] REJECTION
                      [7] CORRESPONDENCE
                      [8] UNCLASSIFIED
                """.trimIndent())
                t.print(brightBlue("select status [1-8] > "))
                if (!scanner.hasNextLine()) return
                val sChoice = scanner.nextLine().trim()
                val newStatus = when (sChoice) {
                    "1" -> "ACKNOWLEDGEMENT"
                    "2" -> "INTERVIEW_REQUEST"
                    "3" -> "INTERVIEW_CONFIRMATION"
                    "4" -> "ASSESSMENT"
                    "5" -> "OFFER"
                    "6" -> "REJECTION"
                    "7" -> "CORRESPONDENCE"
                    else -> "UNCLASSIFIED"
                }
                executeRectify(email.id, email.applicationId, newStatus, isNoise = false)
            }
            "3" -> {
                executeRectify(email.id, null, "NOISE", isNoise = true)
            }
            "4" -> {
                executeRectify(email.id, null, email.status, isNoise = false)
            }
            else -> t.println("Cancelled.")
        }
    }

    private fun executeRectify(messageId: String, targetAppId: String?, status: String, isNoise: Boolean) {
        val firestore = TerminalApp.firestoreInstance
        if (firestore == null) {
            t.println(red("Cannot write rectification in offline/cache mode."))
            return
        }

        t.println(dim("Applying transactional rectification..."))
        try {
            val emailRef = firestore.collection("emails").document(messageId)
            val emailSnap = emailRef.get().get()
            val previousAppId = if (emailSnap.exists()) emailSnap.getString("applicationId") else null

            var resolvedCompanyId: String? = null
            if (!targetAppId.isNullOrBlank()) {
                val appDoc = firestore.collection("applications").document(targetAppId).get().get()
                if (!appDoc.exists()) {
                    t.println(red("Target application '$targetAppId' does not exist in Firestore."))
                    return
                }
                resolvedCompanyId = appDoc.getString("companyId")
            }

            firestore.runTransaction { tx ->
                // Remove from previous application if needed
                if (!previousAppId.isNullOrBlank() && previousAppId != targetAppId) {
                    val prevRef = firestore.collection("applications").document(previousAppId)
                    val prevSnap = tx.get(prevRef).get()
                    if (prevSnap.exists()) {
                        val ids = ((prevSnap.get("emailMessageIds") as? List<*>) ?: emptyList<Any>())
                            .map { it.toString() }
                            .filter { it != messageId }
                        tx.update(prevRef, mapOf(
                            "emailMessageIds" to ids,
                            "updatedAt" to com.google.cloud.Timestamp.now()
                        ))
                    }
                }

                // Add to target application if needed
                if (!targetAppId.isNullOrBlank() && targetAppId != previousAppId) {
                    val targetRef = firestore.collection("applications").document(targetAppId)
                    val targetSnap = tx.get(targetRef).get()
                    if (targetSnap.exists()) {
                        val ids = ((targetSnap.get("emailMessageIds") as? List<*>) ?: emptyList<Any>())
                            .map { it.toString() }
                            .toMutableSet()
                        ids.add(messageId)
                        tx.update(targetRef, mapOf(
                            "emailMessageIds" to ids.toList(),
                            "updatedAt" to com.google.cloud.Timestamp.now()
                        ))
                    }
                }

                // Update email document
                val updates = mutableMapOf<String, Any?>(
                    "id" to messageId,
                    "status" to status,
                    "applicationId" to targetAppId,
                    "companyId" to resolvedCompanyId,
                    "rectifiedAt" to java.time.Instant.now().toString(),
                    "updatedAt" to java.time.Instant.now().toString()
                )
                tx.set(emailRef, updates, SetOptions.merge())
                null
            }.get()

            t.println(green("✓ Rectified $messageId: status=$status, app=${targetAppId ?: "none"}"))
        } catch (e: Exception) {
            t.println(red("Error applying rectification: ${e.message}"))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Job Tracker Terminal App
// ─────────────────────────────────────────────────────────────────────────────

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

    public val firestoreInstance: Firestore? get() = firestore

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

        // 1. Check for roadmap mode
        if (argsList.contains("--roadmap") || argsList.firstOrNull() == "roadmap") {
            val roadmapArgs = argsList.filter { it != "--roadmap" && it != "roadmap" }.toTypedArray()
            dev.fnc314.tools.jobtracker.terminal.roadmap.RoadmapApp.run(roadmapArgs)
            return
        }

        // 2. Check for proposals review mode
        if (argsList.contains("--proposals") || argsList.firstOrNull() == "proposals") {
            val autoYes = argsList.contains("-y") || argsList.contains("--yes")
            runProposalsMode(autoYes = autoYes)
            return
        }

        // 3. Check for emails review/rectification mode
        if (argsList.contains("--emails") || argsList.firstOrNull() == "emails") {
            EmailsApp.run()
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
                    val roundData = (data["currentInterviewRound"] as? Map<*, *>)
                        ?: (data["current_interview_round"] as? Map<*, *>)
                    @Suppress("UNCHECKED_CAST")
                    val progress = InterviewProgress.fromMap(roundData as? Map<String, Any?>)

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
                        rawData = data,
                        interviewProgress = progress
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
                val roundObj = (obj["currentInterviewRound"] as? JsonObject)
                    ?: (obj["current_interview_round"] as? JsonObject)
                val roundMap = roundObj?.let { jsonObjectToMap(it) }
                val progress = InterviewProgress.fromMap(roundMap)

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
                    date = obj["date"]?.jsonPrimitive?.content ?: obj["createdAt"]?.jsonPrimitive?.content ?: "",
                    interviewProgress = progress
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

    private fun jsonArrayToMapList(arr: JsonArray?): List<Map<String, Any?>> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el -> (el as? JsonObject)?.let { jsonObjectToMap(it) } }
    }

    private fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> = obj.mapValues { (_, v) ->
        when (v) {
            is JsonPrimitive -> v.booleanOrNull ?: v.intOrNull ?: v.longOrNull ?: v.content
            is JsonObject -> jsonObjectToMap(v)
            is JsonArray -> v.map { el ->
                when (el) {
                    is JsonPrimitive -> el.booleanOrNull ?: el.intOrNull ?: el.content
                    is JsonObject -> jsonObjectToMap(el)
                    else -> el.toString()
                }
            }
            else -> v.toString()
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
                                unmatchedCount = unmatchedList.size,
                                changes = jsonArrayToMapList(changesList),
                                unmatched = jsonArrayToMapList(unmatchedList)
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
                val changesArray = obj["changes"] as? JsonArray
                val unmatchedArray = obj["unmatched"] as? JsonArray
                val changesCount = obj["changes"]?.jsonPrimitive?.intOrNull ?: changesArray?.size ?: 0
                val unmatchedCount = obj["unmatched"]?.jsonPrimitive?.intOrNull ?: unmatchedArray?.size ?: 0
                ProposalRecord(
                    proposalId = obj["proposalId"]?.jsonPrimitive?.content ?: "",
                    createdAt = obj["createdAt"]?.jsonPrimitive?.content ?: "",
                    state = "PENDING",
                    changesCount = changesCount,
                    unmatchedCount = unmatchedCount,
                    changes = jsonArrayToMapList(changesArray),
                    unmatched = jsonArrayToMapList(unmatchedArray)
                )
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Interactive Proposals Review Mode (--proposals)
    // ─────────────────────────────────────────────────────────────────────────────

    public fun runProposalsMode(autoYes: Boolean = false) {
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
                    "\n" + dim("Review staged inbox proposals • Select: [t <id>] toggle, [all] select all, [none] clear, [i <id>] inspect, [a] approve, [r] reject, [q] quit"),
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

        fun confirmAction(prompt: String): Boolean {
            if (autoYes) return true
            System.err.print(t.render(yellow("$prompt [y/N] > ")))
            if (!scanner.hasNextLine()) return false
            val ans = scanner.nextLine().trim().lowercase()
            return ans == "y" || ans == "yes"
        }

        while (true) {
            System.err.println()
            System.err.print(t.render(brightBlue("proposals [a:approve, r:reject, t <id>:toggle, all, none, i <id>:inspect, q:quit] > ")))
            if (!scanner.hasNextLine()) break
            val line = scanner.nextLine().trim()
            if (line.isEmpty()) continue

            val parts = line.split("\\s+".toRegex())
            val cmd = parts[0].lowercase()

            when (cmd) {
                "q", "quit", "exit" -> break

                "all" -> {
                    proposals.forEach { it.enabled = true }
                    System.err.println(green("Selected all ${proposals.size} proposals."))
                    renderProposalsList()
                }

                "none", "clear" -> {
                    proposals.forEach { it.enabled = false }
                    System.err.println(yellow("Cleared all proposal selections."))
                    renderProposalsList()
                }

                "t", "toggle" -> {
                    if (parts.size > 1) {
                        val targetId = parts[1]
                        when {
                            targetId == "all" -> {
                                proposals.forEach { it.enabled = true }
                                System.err.println(green("Selected all ${proposals.size} proposals."))
                                renderProposalsList()
                            }
                            targetId == "none" -> {
                                proposals.forEach { it.enabled = false }
                                System.err.println(yellow("Cleared all proposal selections."))
                                renderProposalsList()
                            }
                            else -> {
                                val prop = proposals.find { it.proposalId.startsWith(targetId) }
                                if (prop != null) {
                                    prop.enabled = !prop.enabled
                                    renderProposalsList()
                                } else {
                                    System.err.println(red("Proposal not found: $targetId"))
                                }
                            }
                        }
                    } else {
                        System.err.println(yellow("Usage: t <id-prefix> to toggle selection (or 't all' / 't none')"))
                    }
                }

                "i", "inspect" -> {
                    if (parts.size > 1) {
                        inspectProposal(parts[1], proposals)
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
                        System.err.println(yellow("No proposals selected for approval. Use 't <id>' or 'all' to select proposals to approve."))
                        continue
                    }

                    val totalChanges = toApprove.sumOf { it.changesCount }
                    val prompt = if (parts.size > 1) {
                        "Approve proposal ${toApprove.first().proposalId} ($totalChanges change(s))?"
                    } else {
                        "Approve ${toApprove.size} proposal(s) with total $totalChanges change(s)?"
                    }

                    if (!confirmAction(prompt)) {
                        System.err.println(yellow("Approval aborted."))
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
                        System.err.println(yellow("No proposals selected for rejection. Use 't <id>' or 'all' to select proposals to reject."))
                        continue
                    }

                    val prompt = if (parts.size > 1) {
                        "Reject proposal ${toReject.first().proposalId}?"
                    } else {
                        "Reject ${toReject.size} proposal(s)?"
                    }

                    if (!confirmAction(prompt)) {
                        System.err.println(yellow("Rejection aborted."))
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
                    System.err.println(red("Unknown command '$cmd'. Commands: 't <id>' (toggle), 'all', 'none', 'i <id>' (inspect), 'a' (approve), 'r' (reject), 'q' (quit)."))
                }
            }
        }

        // Emit final summary JSON on stdout for tracker.py
        val remaining = proposals.size
        println("""{"ok": true, "remaining_pending": $remaining, "summary": "proposals review complete"}""")
    }

    private fun inspectProposal(proposalIdPrefix: String, inMemoryProposals: List<ProposalRecord> = emptyList()) {
        val target = inMemoryProposals.find { it.proposalId.startsWith(proposalIdPrefix) }
        if (target != null && (target.changes.isNotEmpty() || target.unmatched.isNotEmpty())) {
            renderProposalDetails(target)
            return
        }

        // Try direct Firestore lookup if online
        if (!isOffline && firestore != null) {
            try {
                val doc = firestore!!.collection("pending-proposals").document(proposalIdPrefix).get().get()
                if (doc.exists()) {
                    val data = doc.data ?: emptyMap()
                    val changes = (data["changes"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
                    val unmatched = (data["unmatched"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
                    val prop = ProposalRecord(
                        proposalId = data["proposalId"]?.toString() ?: doc.id,
                        createdAt = data["createdAt"]?.toString() ?: "-",
                        state = data["state"]?.toString() ?: "PENDING",
                        changesCount = changes.size,
                        unmatchedCount = unmatched.size,
                        changes = changes,
                        unmatched = unmatched
                    )
                    renderProposalDetails(prop)
                    return
                }
            } catch (e: Exception) {
                // Fallback below
            }
        }

        // Fallback: subprocess tracker.py proposals show
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
                    val changes = prop["changes"]?.jsonArray ?: JsonArray(emptyList())
                    val unmatched = prop["unmatched"]?.jsonArray ?: JsonArray(emptyList())

                    val propRecord = ProposalRecord(
                        proposalId = pid,
                        createdAt = created,
                        state = prop["state"]?.jsonPrimitive?.content ?: "PENDING",
                        changesCount = changes.size,
                        unmatchedCount = unmatched.size,
                        changes = jsonArrayToMapList(changes),
                        unmatched = jsonArrayToMapList(unmatched)
                    )
                    renderProposalDetails(propRecord)
                    return
                }
            }
            System.err.println(red("Proposal '$proposalIdPrefix' not found."))
        } catch (e: Exception) {
            System.err.println(red("Failed to inspect proposal: ${e.message}"))
        }
    }

    private fun renderProposalDetails(prop: ProposalRecord) {
        val pid = prop.proposalId
        val created = prop.createdAt.ifBlank { "-" }
        val changes = prop.changes
        val unmatched = prop.unmatched

        val headerInfo = buildString {
            appendLine("Proposal ID:  ${bold(pid)}")
            appendLine("Created At:   $created")
            appendLine("State:        ${prop.state}")
            appendLine("Changes:      ${changes.size}")
            appendLine("Unmatched:    ${unmatched.size}")
        }

        val renderedChanges = if (changes.isNotEmpty()) {
            table {
                header {
                    row(cyan("#"), cyan("Application ID"), cyan("Status Transition"), cyan("Confidence"), cyan("Email Subject"))
                }
                body {
                    changes.forEachIndexed { idx, ch ->
                        val appTarget = (ch["applicationId"] as? String)?.takeIf { it.isNotBlank() }
                            ?: (ch["leadCompany"] as? String)?.let { "lead: $it" }
                            ?: "-"
                        val stFrom = cleanStatus((ch["statusFrom"] as? String) ?: "")
                        val stTo = cleanStatus((ch["statusTo"] as? String) ?: "")
                        val trans = when {
                            stFrom.isNotBlank() && stTo.isNotBlank() -> "$stFrom → $stTo"
                            stTo.isNotBlank() -> "→ $stTo"
                            else -> ch["eventType"]?.toString() ?: "-"
                        }
                        val conf = (ch["confidence"] as? String) ?: "medium"
                        val confColor = when (conf.uppercase()) {
                            "HIGH", "HIGH_CONFIDENCE" -> green
                            "MEDIUM", "MEDIUM_CONFIDENCE" -> yellow
                            else -> gray
                        }
                        val srcEmail = ch["sourceEmail"] as? Map<*, *>
                        val subj = (srcEmail?.get("subject") as? String) ?: "(no subject)"
                        row(
                            (idx + 1).toString(),
                            appTarget,
                            trans,
                            confColor(conf.removeSuffix("_CONFIDENCE").lowercase()),
                            subj
                        )
                    }
                }
            }
        } else null

        val renderedUnmatched = if (unmatched.isNotEmpty()) {
            table {
                header {
                    row(yellow("#"), yellow("Subject"), yellow("Reason"))
                }
                body {
                    unmatched.take(15).forEachIndexed { idx, u ->
                        val subj = (u["subject"] as? String) ?: "(no subject)"
                        val reason = (u["reason"] as? String) ?: "-"
                        row((idx + 1).toString(), subj, reason)
                    }
                }
            }
        } else null

        val panelContent = buildString {
            append(headerInfo)
            if (renderedChanges != null) {
                appendLine()
                appendLine(bold(cyan("=== Proposed State Changes (${changes.size}) ===")))
                appendLine(t.render(renderedChanges))
            }
            if (renderedUnmatched != null) {
                appendLine()
                appendLine(bold(yellow("=== Unmatched Emails (${unmatched.size}) ===")))
                appendLine(t.render(renderedUnmatched))
                if (unmatched.size > 15) {
                    appendLine(dim("... and ${unmatched.size - 15} more unmatched emails"))
                }
            }
        }

        System.err.println(t.render(
            Panel(
                content = panelContent.trimEnd(),
                title = cyan("Proposal Details: $pid")
            )
        ))
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
        val pendingProposals = try { loadPendingProposals().size } catch (_: Exception) { 0 }

        t.println(
            white("Filter: ") + (if (openOnly) green("OPEN ONLY") else cyan("ALL")) +
                white(" | Total: ") + bold(totalCount.toString()) +
                white(" | Open: ") + green(openCount.toString()) +
                white(" | Interviews: ") + yellow(interviewCount.toString()) +
                white(" | Offers: ") + bold(green(offerCount.toString())) +
                white(" | Leads: ") + magenta(leadCount.toString()) +
                white(" | Pending Proposals: ") +
                (if (pendingProposals > 0) yellow(bold(pendingProposals.toString())) else dim("0"))
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
                    val baseStatus = doc.status.uppercase()
                    val displayStatus = if ((baseStatus == "INTERVIEW" || baseStatus == "INTERVIEWING") && doc.interviewProgress != null) {
                        "INTERVIEWING (${doc.interviewProgress.roundBadge()})"
                    } else {
                        doc.status
                    }
                    val statusFormatted = when (baseStatus) {
                        "APPLIED", "SUBMITTED" -> green(displayStatus)
                        "INTERVIEW", "INTERVIEWING" -> yellow(displayStatus)
                        "RECRUITER_SCREENED" -> cyan(displayStatus)
                        "OFFER", "HIRED" -> bold(green(displayStatus))
                        "REJECTED" -> red(displayStatus)
                        "WITHDRAWN", "APPLICATION_WITHDRAWN", "NO_RESPONSE" -> dim(displayStatus)
                        "LEAD", "DRAFTING", "DRAFTED" -> magenta(displayStatus)
                        else -> displayStatus
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
            if ((app.status.uppercase() == "INTERVIEW" || app.status.uppercase() == "INTERVIEWING") && app.interviewProgress != null) {
                appendLine("Interview:     ${bold(yellow(app.interviewProgress.formattedSummary()))}")
            }
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
            t.print(brightBlue("tracker [o:open, l:leads, s <status>, / <query>, i <id>, b <id>, r <id>, p:proposals, q:quit] > "))
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
                "e", "emails" -> {
                    EmailsApp.run()
                    listApplications(openOnly, filterQuery, statusFilter, tagFilter, limit, sortCol)
                }
                "p", "proposals" -> {
                    TerminalApp.runProposalsMode()
                    listApplications(openOnly, filterQuery, statusFilter, tagFilter, limit, sortCol)
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
                          p, proposals        - Review staged inbox proposals (approve/reject)
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