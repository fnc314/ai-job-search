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
import java.util.Scanner

public object TerminalApp {
    private val t: Terminal = Terminal()
    private val firestore: Firestore by lazy {
        RemotePlatform.initialize()
        val app = FirebaseApp.getInstance()
        FirestoreClient.getFirestore(app)
    }

    public fun run(args: Array<String>) {
        if (args.isNotEmpty()) {
            when (args[0]) {
                "inspect", "get" -> {
                    if (args.size > 1) {
                        inspectApplication(args[1])
                    } else {
                        t.println(red("Usage: tui inspect <id>"))
                    }
                    return
                }
                "open", "--open" -> {
                    listApplications(openOnly = true)
                    return
                }
                "list", "--list" -> {
                    listApplications(openOnly = false)
                    return
                }
            }
        }

        // Interactive Loop
        interactiveLoop()
    }

    private fun renderHeader() {
        t.println()
        val banner = Panel(
            content = bold(brightBlue("AI Job Search Tracker — Kotlin Terminal Interface")) +
                "\n" + dim("Monorepo Architecture (records + backend + frontend) • Scale-to-Zero Cloud Run"),
            title = cyan("★ Job Application Pipeline ★")
        )
        t.println(banner)
    }

    private fun fetchApplications(openOnly: Boolean = false): List<Map<String, Any?>> {
        var query = firestore.collection("applications") as com.google.cloud.firestore.Query
        if (openOnly) {
            query = query.whereEqualTo("isOpen", true)
        }
        val snapshot = query.get().get()
        return snapshot.documents.map { doc ->
            val data = doc.data
            data["id"] = doc.id
            data
        }
    }

    private fun listApplications(openOnly: Boolean = false, filterQuery: String? = null) {
        renderHeader()
        t.println(dim("Fetching applications from Firestore..."))

        val docs = fetchApplications(openOnly).filter { doc ->
            if (filterQuery.isNullOrBlank()) true
            else {
                val q = filterQuery.lowercase()
                val id = (doc["id"]?.toString() ?: "").lowercase()
                val company = (doc["company"]?.toString() ?: "").lowercase()
                val role = (doc["role"]?.toString() ?: "").lowercase()
                id.contains(q) || company.contains(q) || role.contains(q)
            }
        }

        val totalCount = docs.size
        val openCount = docs.count { it["isOpen"] == true }
        val interviewCount = docs.count { it["status"]?.toString()?.uppercase() == "INTERVIEW" }
        val offerCount = docs.count { it["status"]?.toString()?.uppercase() == "OFFER" }

        t.println(
            white("Filter: ") + (if (openOnly) green("OPEN ONLY") else cyan("ALL")) +
                white(" | Total: ") + bold(totalCount.toString()) +
                white(" | Open: ") + green(openCount.toString()) +
                white(" | Interviews: ") + yellow(interviewCount.toString()) +
                white(" | Offers: ") + green(offerCount.toString())
        )
        t.println()

        if (docs.isEmpty()) {
            t.println(gray("No applications found."))
            return
        }

        val tbl = table {
            header {
                row(cyan("ID"), cyan("Company"), cyan("Role"), cyan("Status"), cyan("Fit"), cyan("Sync Cues"), cyan("Emails"))
            }
            body {
                docs.forEach { doc ->
                    val id = doc["id"]?.toString() ?: ""
                    val company = doc["company"]?.toString() ?: ""
                    val role = doc["role"]?.toString() ?: ""
                    val status = doc["status"]?.toString() ?: ""
                    val fit = doc["fitRating"]?.toString() ?: "-"
                    val emailList = (doc["emailMessageIds"] as? List<*>) ?: emptyList<Any>()

                    val statusFormatted = when (status.uppercase()) {
                        "APPLIED" -> green(status)
                        "INTERVIEW" -> yellow(status)
                        "OFFER", "HIRED" -> bold(green(status))
                        "REJECTED" -> red(status)
                        else -> status
                    }

                    // Visual indicator badge
                    val syncCue = if (emailList.isNotEmpty()) {
                        green("[AUTO-SYNCED]")
                    } else {
                        dim("-")
                    }

                    val emailsBadge = if (emailList.isNotEmpty()) {
                        cyan("${emailList.size} linked")
                    } else {
                        dim("0")
                    }

                    row(id, company, role, statusFormatted, fit, syncCue, emailsBadge)
                }
            }
        }
        t.println(tbl)
    }

    public fun inspectApplication(applicationId: String) {
        val docRef = firestore.collection("applications").document(applicationId)
        val snap = docRef.get().get()

        if (!snap.exists()) {
            t.println(red("Application not found: $applicationId"))
            return
        }

        val data = snap.data ?: emptyMap<String, Any?>()
        val emailIds = (data["emailMessageIds"] as? List<*>) ?: emptyList<Any>()

        val detailsContent = buildString {
            appendLine("Company:       ${bold(data["company"]?.toString() ?: "-")}")
            appendLine("Role:          ${bold(data["role"]?.toString() ?: "-")}")
            appendLine("Status:        ${data["status"] ?: "-"}")
            appendLine("Channel:       ${data["channel"] ?: "-"}")
            appendLine("Location:      ${data["location"] ?: "-"}")
            appendLine("Fit Rating:    ${data["fitRating"] ?: "-"}")
            appendLine("UUIDv7:        ${data["uuid"] ?: "-"}")
            appendLine("Job URL:       ${data["jobUrl"] ?: "-"}")
            appendLine("Portal URL:    ${data["jobApplicationUrl"] ?: "-"}")
        }

        t.println(
            Panel(
                content = detailsContent,
                title = cyan("Application: $applicationId")
            )
        )

        // Related Emails Card
        val emailCardContent = if (emailIds.isEmpty()) {
            gray("No linked email messages recorded.")
        } else {
            emailIds.joinToString("\n") { "  • ${green(it.toString())} ${dim("(Processed & Linked)")}" }
        }

        t.println(
            Panel(
                content = emailCardContent,
                title = green("✉ Related Emails (${emailIds.size})")
            )
        )

        // Timeline Events Card
        val eventsSnap = docRef.collection("events").orderBy("ts").get().get()
        t.println()
        t.println(cyan("=== Timeline History (${eventsSnap.size()} events) ==="))
        if (eventsSnap.isEmpty) {
            t.println(gray("No timeline events recorded."))
        } else {
            val eventsTbl = table {
                header {
                    row(cyan("Timestamp"), cyan("Type"), cyan("Actor"), cyan("Email Msg ID"), cyan("Notes"))
                }
                body {
                    eventsSnap.documents.forEach { evDoc ->
                        val ev = evDoc.data
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

    private fun interactiveLoop() {
        var openOnly = true
        listApplications(openOnly = openOnly)

        val scanner = Scanner(System.`in`)
        while (true) {
            t.println()
            t.print(brightBlue("jobtracker [l:list, o:open/all, i <id>:inspect, q:quit] > "))
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
                    listApplications(openOnly = openOnly)
                }
                "l", "list" -> {
                    val filter = if (parts.size > 1) parts.drop(1).joinToString(" ") else null
                    listApplications(openOnly = openOnly, filterQuery = filter)
                }
                "i", "inspect", "get" -> {
                    if (parts.size > 1) {
                        inspectApplication(parts[1])
                    } else {
                        t.println(yellow("Please specify an ID to inspect. e.g. 'i slickdeals--sr-mobile-engineer-android'"))
                    }
                }
                "h", "help" -> {
                    t.println(
                        """
                        Available Commands:
                          l, list [filter]  - List applications (optional filter by company/role)
                          o, open           - Toggle between Open Only and All applications
                          i, inspect <id>   - View full application details, related emails, and timeline
                          q, quit           - Exit the TUI
                        """.trimIndent()
                    )
                }
                else -> {
                    t.println(red("Unknown command: '$cmd'. Type 'h' for help."))
                }
            }
        }
    }
}

public fun main(args: Array<String>) {
    TerminalApp.run(args)
}
