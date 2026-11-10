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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Scanner
import java.util.concurrent.TimeUnit

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
    var enabled: Boolean = false,
    val changes: List<Map<String, Any?>> = emptyList(),
    val unmatched: List<Map<String, Any?>> = emptyList()
)

// ─────────────────────────────────────────────────────────────────────────────
// Roadmap Data Models
// ─────────────────────────────────────────────────────────────────────────────

public data class RoadmapItem(
    val id: String,
    val title: String,
    val shortDesc: String,
    val criticality: String,
    val priority: String,
    val complexity: Int,
    val status: String,
    val completionPercent: Int,
    val creationDate: String,
    val objective: String,
    val scope: String,
    val dependencies: String,
    val acceptanceCriteria: String,
    val notes: String,
    val linkedPlans: List<String>,
    val changelog: List<RoadmapChangelogEntry>
)

public data class RoadmapChangelogEntry(
    val date: String,
    val change: String,
    val details: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Roadmap Parser & Writer
// ─────────────────────────────────────────────────────────────────────────────

public object RoadmapParser {
    private val idPattern = Regex("### \\[(RM-\\d+)\\]\\s+(.+)")
    private val fieldPattern = Regex("^\\*\\*(.+?):\\*\\*\\s+(.+)$")
    private val frozenSection = Regex("^## Frozen Items", RegexOption.MULTILINE)

    public fun parse(roadmapFile: File, plansReadme: File? = null): List<RoadmapItem> {
        if (!roadmapFile.exists()) return emptyList()
        val content = roadmapFile.readText()

        // Check file size guard
        val sizeKb = content.length / 1024.0
        if (sizeKb > 50) {
            System.err.println(yellow("⚠ ROADMAP.md is ${sizeKb.toInt()}KB (limit ~50KB). Consider freezing old items."))
        }

        // Find the frozen section — items after it are frozen
        val frozenMatch = frozenSection.find(content)
        val activeContent = if (frozenMatch != null) {
            content.substring(0, frozenMatch.range.first)
        } else {
            content
        }

        // Split into item blocks by "### [RM-"
        val itemBlocks = activeContent.split(Regex("(?=### \\[RM-\\d+\\])")).filter {
            it.contains("### [RM-")
        }

        val items = itemBlocks.mapNotNull { block -> parseItemBlock(block, plansReadme) }
        return items
    }

    private fun parseItemBlock(block: String, plansReadme: File? = null): RoadmapItem? {
        val idMatch = idPattern.find(block) ?: return null
        val id = idMatch.groupValues[1]
        val title = idMatch.groupValues[2].trim()

        val fields = mutableMapOf<String, String>()
        var currentSection: String? = null
        val sectionBuilder = StringBuilder()

        for (line in block.lines().drop(1)) {
            val fieldMatch = fieldPattern.matchEntire(line)
            if (fieldMatch != null) {
                // Save previous section
                if (currentSection != null) {
                    fields[currentSection] = sectionBuilder.toString().trim()
                    sectionBuilder.clear()
                }
                fields[fieldMatch.groupValues[1]] = fieldMatch.groupValues[2].trim()
                currentSection = null
            } else if (line.startsWith("**Linked Plans**")) {
                if (currentSection != null) {
                    fields[currentSection] = sectionBuilder.toString().trim()
                    sectionBuilder.clear()
                }
                currentSection = "LinkedPlans"
            } else if (line.startsWith("**Changelog**")) {
                if (currentSection != null) {
                    fields[currentSection] = sectionBuilder.toString().trim()
                    sectionBuilder.clear()
                }
                currentSection = "Changelog"
            } else if (currentSection != null) {
                sectionBuilder.appendLine(line)
            }
        }
        if (currentSection != null) {
            fields[currentSection] = sectionBuilder.toString().trim()
        }

        // Parse linked plans
        val linkedPlans = parseLinkedPlans(fields["LinkedPlans"], plansReadme)

        // Parse changelog
        val changelog = parseChangelog(fields["Changelog"])

        val complexity = fields["Complexity"]?.split("—")?.firstOrNull()?.trim()?.toIntOrNull()
            ?: fields["Complexity"]?.toIntOrNull() ?: 0

        return RoadmapItem(
            id = id,
            title = title,
            shortDesc = fields["Short Desc"] ?: "",
            criticality = fields["Criticality"] ?: "NOT_SET",
            priority = fields["Priority"] ?: "NOT_SET",
            complexity = complexity,
            status = fields["Status"] ?: "OPEN",
            completionPercent = fields["Completion %"]?.toIntOrNull() ?: 0,
            creationDate = fields["Creation Date"] ?: "",
            objective = fields["Objective"] ?: "",
            scope = fields["Scope"] ?: "",
            dependencies = fields["Dependencies"] ?: "None",
            acceptanceCriteria = fields["Acceptance Criteria"] ?: "",
            notes = fields["Notes"] ?: "",
            linkedPlans = linkedPlans,
            changelog = changelog
        )
    }

    private fun parseLinkedPlans(raw: String?, plansReadme: File?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val plans = raw.lines().mapNotNull {
            val trimmed = it.trim().removePrefix("- ")
            if (trimmed.isNotEmpty() && trimmed != "None") trimmed else null
        }
        return plans
    }

    private fun parseChangelog(raw: String?): List<RoadmapChangelogEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        val entries = mutableListOf<RoadmapChangelogEntry>()
        val datePattern = Regex("^\\|\\s*(\\d{4}-\\d{2}-\\d{2})\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*$")

        for (line in raw.lines()) {
            val match = datePattern.matchEntire(line)
            if (match != null) {
                entries.add(RoadmapChangelogEntry(
                    date = match.groupValues[1],
                    change = match.groupValues[2].trim(),
                    details = match.groupValues[3].trim()
                ))
            }
        }
        return entries
    }

    public fun findLinkedPlansForItem(itemId: String, plansReadme: File): List<String> {
        if (!plansReadme.exists()) return emptyList()
        val content = plansReadme.readText()
        val plans = mutableListOf<String>()
        for (line in content.lines()) {
            if (line.contains(itemId, ignoreCase = true)) {
                // Extract the plan path from the markdown link
                val linkPattern = Regex("\\[`([^`]+)`\\]\\(\\.?/([^)]+)\\)")
                val match = linkPattern.find(line)
                if (match != null) {
                    plans.add(match.groupValues[2])
                }
            }
        }
        return plans
    }
}

public object RoadmapWriter {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    public fun addItem(roadmapFile: File, item: RoadmapItem): Boolean {
        val content = if (roadmapFile.exists()) roadmapFile.readText() else ""
        val frozenMatch = Regex("^## Frozen Items", RegexOption.MULTILINE).find(content)
        val insertPoint = frozenMatch?.range?.first ?: content.length

        val itemBlock = buildString {
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("### [${item.id}] ${item.title}")
            appendLine()
            appendLine("**Short Desc:** ${item.shortDesc}")
            appendLine("**Criticality:** ${item.criticality}")
            appendLine("**Priority:** ${item.priority}")
            appendLine("**Complexity:** ${item.complexity} — ${complexityLabel(item.complexity)}")
            appendLine("**Status:** ${item.status}")
            appendLine("**Completion %:** ${item.completionPercent}")
            appendLine("**Creation Date:** ${item.creationDate}")
            appendLine("**Objective:** ${item.objective}")
            appendLine()
            if (item.scope.isNotBlank()) {
                appendLine(item.scope)
                appendLine()
            }
            if (item.dependencies.isNotBlank()) {
                appendLine("**Dependencies:** ${item.dependencies}")
                appendLine()
            }
            if (item.acceptanceCriteria.isNotBlank()) {
                appendLine("**Acceptance Criteria:**")
                appendLine(item.acceptanceCriteria)
                appendLine()
            }
            if (item.notes.isNotBlank()) {
                appendLine("**Notes:**")
                appendLine(item.notes)
                appendLine()
            }
            appendLine("**Linked Plans:**")
            if (item.linkedPlans.isEmpty()) {
                appendLine("- None")
            } else {
                item.linkedPlans.forEach { appendLine("- $it") }
            }
            appendLine()
            appendLine("**Changelog:**")
            appendLine("| Date       | Change            | Details                              |")
            appendLine("|------------|-------------------|--------------------------------------|")
            item.changelog.forEach {
                appendLine("| ${it.date} | ${it.change}      | ${it.details}                        |")
            }
        }

        val updated = content.substring(0, insertPoint) + itemBlock + content.substring(insertPoint)
        roadmapFile.writeText(updated)
        return true
    }

    public fun updateField(roadmapFile: File, itemId: String, field: String, value: String): Boolean {
        val content = roadmapFile.readText()
        val itemStart = content.indexOf("### [$itemId]")
        if (itemStart == -1) return false

        // Find the next item or frozen section
        val nextItem = Regex("### \\[RM-\\d+\\]").find(content, itemStart + 1)
        val frozenSection = Regex("## Frozen Items").find(content, itemStart + 1)
        val endBound = minOf(
            nextItem?.range?.first ?: Int.MAX_VALUE,
            frozenSection?.range?.first ?: Int.MAX_VALUE,
            content.length
        )
        val itemBlock = content.substring(itemStart, endBound)

        val fieldKey = when (field.lowercase()) {
            "criticality" -> "Criticality"
            "priority" -> "Priority"
            "status" -> "Status"
            "completion" -> "Completion %"
            else -> field
        }

        val fieldPattern = Regex("(^\\*\\*$fieldKey:\\*\\*\\s+).*$", RegexOption.MULTILINE)
        val updatedBlock = itemBlock.replace(fieldPattern, "$1$value")

        if (updatedBlock == itemBlock) {
            System.err.println(yellow("⚠ Could not find field '$fieldKey' in $itemId"))
            return false
        }

        val updated = content.substring(0, itemStart) + updatedBlock + content.substring(endBound)
        roadmapFile.writeText(updated)

        // Append changelog entry
        appendChangelog(roadmapFile, itemId, field, value)
        return true
    }

    public fun appendChangelog(roadmapFile: File, itemId: String, change: String, details: String) {
        val content = roadmapFile.readText()
        val itemStart = content.indexOf("### [$itemId]")
        if (itemStart == -1) return

        val nextItem = Regex("### \\[RM-\\d+\\]").find(content, itemStart + 1)
        val frozenSection = Regex("## Frozen Items").find(content, itemStart + 1)
        val endBound = minOf(
            nextItem?.range?.first ?: Int.MAX_VALUE,
            frozenSection?.range?.first ?: Int.MAX_VALUE,
            content.length
        )
        val itemBlock = content.substring(itemStart, endBound)

        val today = LocalDate.now().format(dateFormat)
        val changelogLine = "| $today | $change      | $details                        |"

        // Find the changelog table and append the line
        val changelogStart = itemBlock.indexOf("**Changelog:**")
        if (changelogStart != -1) {
            // Find the last line of the changelog table
            val changelogSection = itemBlock.substring(changelogStart)
            val lines = changelogSection.lines()
            var lastDataLine = -1
            for (i in lines.indices) {
                if (lines[i].startsWith("|")) {
                    lastDataLine = i
                }
            }
            if (lastDataLine >= 0) {
                // Find the last changelog data line and insert after it
                val changelogPattern = Regex("(\\|\\s*\\d{4}-\\d{2}-\\d{2}.*\\|\\n)(?=\\n)")
                val updatedBlock = changelogPattern.replace(itemBlock, "$1$changelogLine\n")
                val updated = content.substring(0, itemStart) + updatedBlock + content.substring(endBound)
                roadmapFile.writeText(updated)
            }
        }
    }

    public fun freezeAllItems(roadmapFile: File, reason: String): Boolean {
        val content = roadmapFile.readText()
        val frozenMatch = Regex("^## Frozen Items", RegexOption.MULTILINE).find(content)
        if (frozenMatch == null) return false

        val activeContent = content.substring(0, frozenMatch.range.first).trim()
        val frozenSection = content.substring(frozenMatch.range.first)

        if (activeContent.isBlank()) return true

        val today = LocalDate.now().format(dateFormat)
        val frozenBlock = buildString {
            appendLine("## Frozen Items")
            appendLine()
            appendLine("Items in this section have been frozen via the nuclear clear roadmap option. They are archived and should not be considered active work items.")
            appendLine()
            appendLine("**Frozen on:** $today")
            appendLine("**Reason:** $reason")
            appendLine()
            appendLine("---")
            appendLine()

            // Parse and rewrite each item with frozen status
            val itemBlocks = activeContent.split(Regex("(?=### \\[RM-\\d+\\])")).filter { it.contains("### [RM-") }
            for (block in itemBlocks) {
                val frozenItem = block.replace(
                    Regex("(^\\*\\*Status:\\*\\*\\s+).*$", RegexOption.MULTILINE),
                    "$1FROZEN"
                ).replace(
                    Regex("(^\\*\\*Completion %:\\*\\*\\s+).*$", RegexOption.MULTILINE),
                    "$10"
                )
                appendLine(frozenItem)
                appendLine()
                appendLine("---")
                appendLine()
            }
        }

        // Remove the old frozen section and replace with new one
        val updated = content.substring(0, frozenMatch.range.first) + frozenSection.replace(
            Regex("## Frozen Items.*", RegexOption.DOT_MATCHES_ALL),
            frozenBlock
        )

        // Clear active items (keep only header, scales, TOC, and frozen section)
        val headerEnd = content.indexOf("---", frozenMatch.range.first - 500)
        if (headerEnd > 0) {
            // Keep header through TOC, then frozen section
            val tocEnd = content.indexOf("---", frozenMatch.range.first - 100)
            if (tocEnd > 0) {
                val header = content.substring(0, tocEnd + 3)
                val finalContent = header + "\n\n" + frozenBlock
                roadmapFile.writeText(finalContent)
            }
        }

        return true
    }

    private fun complexityLabel(level: Int): String {
        return when (level) {
            1 -> "Trivial"
            2 -> "Simple"
            3 -> "Moderate"
            4 -> "Complex"
            5 -> "Critical"
            else -> "Unknown"
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Roadmap Interactive TUI
// ─────────────────────────────────────────────────────────────────────────────

public object RoadmapApp {
    private val t: Terminal = Terminal()
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val scanner = Scanner(System.`in`)
    private var selectedItem: RoadmapItem? = null

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

    private val repoRoot: File by lazy { findRepoRoot() }
    private val roadmapFile: File by lazy { File(repoRoot, "ROADMAP.md") }
    private val plansReadme: File by lazy { File(repoRoot, ".agents/plans/README.md") }

    public fun run() {
        if (!roadmapFile.exists()) {
            t.println(red("ROADMAP.md not found. Nothing to manage."))
            return
        }

        var items = RoadmapParser.parse(roadmapFile, plansReadme.takeIf { it.exists() })
        var currentView: String = "list" // list, detail, add
        selectedItem = null

        while (true) {
            when (currentView) {
                "list" -> {
                    currentView = renderListView(items)
                    if (currentView == "detail") {
                        // selectedItem already set
                    }
                }
                "detail" -> {
                    if (selectedItem != null) {
                        val next = renderDetailView(selectedItem!!, items)
                        if (next == "updated") {
                            items = RoadmapParser.parse(roadmapFile, plansReadme.takeIf { it.exists() })
                        }
                        currentView = if (next == "updated" || next == "back") "list" else currentView
                    } else {
                        currentView = "list"
                    }
                }
                "add" -> {
                    val added = renderAddForm(items)
                    if (added != null) {
                        items = RoadmapParser.parse(roadmapFile, plansReadme.takeIf { it.exists() })
                    }
                    currentView = "list"
                }
                "exit" -> return
            }
        }
    }

    private fun renderListView(items: List<RoadmapItem>): String {
        t.println()
        t.println(t.render(
            Panel(
                content = bold(brightBlue("Roadmap Manager")) + "\n" +
                    dim("Select an item to inspect • + to add • clear to freeze all • q to quit"),
                title = cyan("★ Roadmap ★")
            )
        ))

        t.println()
        t.println(cyan("ACTIVE ITEMS") + white(" | ") + bold("${items.size} items"))
        t.println()

        if (items.isEmpty()) {
            t.println(gray("No active roadmap items. Press + to add one."))
            t.println()
            t.print(brightBlue("roadmap [+ add, q quit] > "))
            if (!scanner.hasNextLine()) return "exit"
            val line = scanner.nextLine().trim()
            return when {
                line == "+" || line == "add" -> "add"
                line == "q" || line == "quit" -> "exit"
                else -> "list"
            }
        }

        val tbl = table {
            header {
                row(cyan("ID"), cyan("Name"), cyan("Short Desc"), cyan("Criticality"), cyan("Age"))
            }
            body {
                items.forEach { item ->
                    val age = calculateAge(item.creationDate)
                    val critColor = when (item.criticality.uppercase()) {
                        "PIVOTAL" -> bold(red(item.criticality))
                        "HIGH" -> yellow(item.criticality)
                        "MEDIUM" -> item.criticality
                        "LOW" -> gray(item.criticality)
                        else -> item.criticality
                    }
                    row(item.id, item.title, item.shortDesc, critColor, age)
                }
            }
        }
        t.println(t.render(tbl))

        t.println()
        t.print(brightBlue("roadmap [id to inspect, + add, clear, q quit] > "))
        if (!scanner.hasNextLine()) return "exit"
        val line = scanner.nextLine().trim()

        return when {
            line == "q" || line == "quit" -> "exit"
            line == "+" || line == "add" -> "add"
            line == "clear" -> {
                handleClearRoadmap(items)
                "list"
            }
            items.any { it.id.lowercase() == line.lowercase() || it.id == line } -> {
                selectedItem = items.find { it.id.lowercase() == line.lowercase() || it.id == line }
                "detail"
            }
            else -> {
                t.println(red("Unknown item: $line"))
                "list"
            }
        }
    }

    private fun renderDetailView(item: RoadmapItem, items: List<RoadmapItem>): String {
        t.println()
        val statusColor = when (item.status.uppercase()) {
            "DONE" -> green(item.status)
            "IN_PROGRESS" -> yellow(item.status)
            "OPEN" -> cyan(item.status)
            "CANCELLED" -> gray(item.status)
            "FROZEN" -> dim(item.status)
            else -> item.status
        }

        val content = buildString {
            appendLine("Title:         ${bold(item.title)}")
            appendLine("Short Desc:    ${item.shortDesc}")
            appendLine("Criticality:   ${item.criticality}")
            appendLine("Priority:      ${item.priority}")
            appendLine("Complexity:    ${item.complexity} — ${complexityLabel(item.complexity)}")
            appendLine("Status:        $statusColor")
            appendLine("Completion:    ${item.completionPercent}%")
            appendLine("Created:       ${item.creationDate}")
            appendLine("Age:           ${calculateAge(item.creationDate)}")
            appendLine()
            appendLine(bold("Objective:"))
            appendLine(item.objective)
            if (item.dependencies.isNotBlank() && item.dependencies != "None") {
                appendLine()
                appendLine(bold("Dependencies:"))
                appendLine(item.dependencies)
            }
        }

        t.println(t.render(
            Panel(content = content, title = cyan("Item: ${item.id}"))
        ))

        // Linked Plans
        if (item.linkedPlans.isNotEmpty()) {
            val plansContent = item.linkedPlans.joinToString("\n") { "  • ${green(it)}" }
            t.println(t.render(
                Panel(content = plansContent, title = green("Linked Plans (${item.linkedPlans.size})"))
            ))
        } else {
            t.println(t.render(
                Panel(content = gray("No linked plans"), title = gray("Linked Plans"))
            ))
        }

        // Changelog
        if (item.changelog.isNotEmpty()) {
            t.println()
            t.println(cyan("=== Changelog (${item.changelog.size} entries) ==="))
            val clTbl = table {
                header {
                    row(cyan("Date"), cyan("Change"), cyan("Details"))
                }
                body {
                    item.changelog.forEach { entry ->
                        row(entry.date, entry.change, entry.details)
                    }
                }
            }
            t.println(t.render(clTbl))
        }

        t.println()
        t.print(brightBlue("${item.id} [c:criticality, p:priority, d:done, x:cancel, b:back] > "))
        if (!scanner.hasNextLine()) return "back"
        val line = scanner.nextLine().trim()

        return when {
            line == "b" || line == "back" -> "back"
            line == "c" || line == "criticality" -> {
                updateCriticality(item)
                "updated"
            }
            line == "p" || line == "priority" -> {
                updatePriority(item)
                "updated"
            }
            line == "d" || line == "done" -> {
                markComplete(item)
                "updated"
            }
            line == "x" || line == "cancel" -> {
                markCancelled(item)
                "updated"
            }
            else -> {
                t.println(red("Unknown command. Use: c, p, d, x, b"))
                "detail"
            }
        }
    }

    private fun renderAddForm(items: List<RoadmapItem>): RoadmapItem? {
        t.println()
        t.println(t.render(
            Panel(content = bold(brightBlue("Add New Roadmap Item")) + "\n" +
                dim("Fill in the fields below. ID and date will be auto-generated."),
                title = cyan("★ New Item ★")
            )
        ))

        // Auto-generate next ID
        val nextNum = (items.mapNotNull { it.id.substringAfter("RM-").toIntOrNull() }.maxOrNull() ?: 0) + 1
        val newId = String.format("RM-%03d", nextNum)
        val today = LocalDate.now().format(dateFormat)

        t.println()
        t.print(brightBlue("Name: "))
        if (!scanner.hasNextLine()) return null
        val name = scanner.nextLine().trim()
        if (name.isBlank()) {
            t.println(yellow("Name is required. Cancelled."))
            return null
        }

        t.print(brightBlue("Description (long): "))
        if (!scanner.hasNextLine()) return null
        val description = scanner.nextLine().trim()
        if (description.isBlank()) {
            t.println(yellow("Description is required. Cancelled."))
            return null
        }

        t.println()
        t.println("Criticality options: PIVOTAL, HIGH, MEDIUM, LOW, NOT_SET")
        t.print(brightBlue("Criticality: "))
        if (!scanner.hasNextLine()) return null
        val criticality = scanner.nextLine().trim().uppercase()
        if (criticality !in listOf("PIVOTAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) {
            t.println(yellow("Invalid criticality. Defaulting to NOT_SET."))
        }
        val finalCriticality = if (criticality in listOf("PIVOTAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) criticality else "NOT_SET"

        t.println()
        t.println("Priority options: CRITICAL, HIGH, MEDIUM, LOW, NOT_SET")
        t.print(brightBlue("Priority: "))
        if (!scanner.hasNextLine()) return null
        val priority = scanner.nextLine().trim().uppercase()
        if (priority !in listOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) {
            t.println(yellow("Invalid priority. Defaulting to NOT_SET."))
        }
        val finalPriority = if (priority in listOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) priority else "NOT_SET"

        // Generate short description
        t.println()
        t.println(yellow("Generating short description..."))
        val generatedShortDesc = generateShortDescription(name, description, finalCriticality)
        t.println()
        t.println(green("Generated: ") + generatedShortDesc)
        t.println()
        t.print(brightBlue("[a]ccept, [e]dit, [r]eplace: "))
        if (!scanner.hasNextLine()) return null
        val shortDescChoice = scanner.nextLine().trim().lowercase()

        val shortDesc = when (shortDescChoice) {
            "a", "accept" -> generatedShortDesc
            "e", "edit" -> {
                t.print(brightBlue("Edit short description: "))
                if (!scanner.hasNextLine()) return null
                val edited = scanner.nextLine().trim()
                if (edited.isBlank()) generatedShortDesc else edited
            }
            "r", "replace" -> {
                t.print(brightBlue("Enter new short description: "))
                if (!scanner.hasNextLine()) return null
                val replaced = scanner.nextLine().trim()
                if (replaced.isBlank()) {
                    t.println(yellow("Empty replacement. Using generated version."))
                    generatedShortDesc
                } else replaced
            }
            else -> {
                t.println(yellow("Invalid choice. Using generated version."))
                generatedShortDesc
            }
        }

        // Confirm
        t.println()
        t.println(bold("Summary:"))
        t.println("  ID:          $newId")
        t.println("  Name:        $name")
        t.println("  Short Desc:  $shortDesc")
        t.println("  Criticality: $finalCriticality")
        t.println("  Priority:    $finalPriority")
        t.println("  Created:     $today")
        t.println()
        t.print(brightBlue("[c]reate or [c]ancel: "))
        if (!scanner.hasNextLine()) return null
        val confirm = scanner.nextLine().trim().lowercase()

        if (confirm == "c" || confirm == "create") {
            val newItem = RoadmapItem(
                id = newId,
                title = name,
                shortDesc = shortDesc,
                criticality = finalCriticality,
                priority = finalPriority,
                complexity = 0,
                status = "OPEN",
                completionPercent = 0,
                creationDate = today,
                objective = description,
                scope = "",
                dependencies = "None",
                acceptanceCriteria = "",
                notes = "",
                linkedPlans = emptyList(),
                changelog = listOf(RoadmapChangelogEntry(today, "Item created", "Initial entry"))
            )
            RoadmapWriter.addItem(roadmapFile, newItem)
            t.println(green("✔ Created $newId: $name"))
            return newItem
        } else {
            t.println(yellow("Cancelled."))
            return null
        }
    }

    private fun handleClearRoadmap(items: List<RoadmapItem>) {
        if (items.isEmpty()) {
            t.println(yellow("No active items to freeze."))
            return
        }

        t.println()
        t.println(red(bold("⚠ NUCLEAR OPTION: Clear Roadmap")))
        t.println(red("This will freeze all ${items.size} active roadmap items."))
        t.println(red("They will be archived and hidden from the active list."))
        t.println()

        // Confirmation 1
        t.print(brightBlue("Type 'CLEAR' to continue: "))
        if (!scanner.hasNextLine()) return
        if (scanner.nextLine().trim() != "CLEAR") {
            t.println(yellow("Cancelled."))
            return
        }

        // Confirmation 2
        t.print(brightBlue("Type 'CLEAR ROADMAP' to continue: "))
        if (!scanner.hasNextLine()) return
        if (scanner.nextLine().trim() != "CLEAR ROADMAP") {
            t.println(yellow("Cancelled."))
            return
        }

        // Confirmation 3
        t.print(brightBlue("Type 'CLEAR ALL' to confirm: "))
        if (!scanner.hasNextLine()) return
        if (scanner.nextLine().trim() != "CLEAR ALL") {
            t.println(yellow("Cancelled."))
            return
        }

        // Reason (min 50 chars)
        t.println()
        t.print(brightBlue("Provide a reason (minimum 50 characters): "))
        if (!scanner.hasNextLine()) return
        var reason = scanner.nextLine().trim()
        while (reason.length < 50) {
            t.println(yellow("Reason must be at least 50 characters (currently ${reason.length})."))
            t.print(brightBlue("Reason: "))
            if (!scanner.hasNextLine()) return
            reason = scanner.nextLine().trim()
        }

        RoadmapWriter.freezeAllItems(roadmapFile, reason)
        t.println()
        t.println(green(bold("✔ Roadmap frozen! All ${items.size} items have been archived.")))
        t.println(dim("Reason: $reason"))
    }

    private fun updateCriticality(item: RoadmapItem) {
        t.println()
        t.println("Current criticality: ${item.criticality}")
        t.println("Options: PIVOTAL, HIGH, MEDIUM, LOW, NOT_SET")
        t.print(brightBlue("New criticality: "))
        if (!scanner.hasNextLine()) return
        val value = scanner.nextLine().trim().uppercase()
        if (value in listOf("PIVOTAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) {
            RoadmapWriter.updateField(roadmapFile, item.id, "criticality", value)
            t.println(green("✔ Updated criticality to $value"))
        } else {
            t.println(yellow("Invalid criticality."))
        }
    }

    private fun updatePriority(item: RoadmapItem) {
        t.println()
        t.println("Current priority: ${item.priority}")
        t.println("Options: CRITICAL, HIGH, MEDIUM, LOW, NOT_SET")
        t.print(brightBlue("New priority: "))
        if (!scanner.hasNextLine()) return
        val value = scanner.nextLine().trim().uppercase()
        if (value in listOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) {
            RoadmapWriter.updateField(roadmapFile, item.id, "priority", value)
            t.println(green("✔ Updated priority to $value"))
        } else {
            t.println(yellow("Invalid priority."))
        }
    }

    private fun markComplete(item: RoadmapItem) {
        RoadmapWriter.updateField(roadmapFile, item.id, "status", "DONE")
        RoadmapWriter.updateField(roadmapFile, item.id, "completion", "100")
        RoadmapWriter.appendChangelog(roadmapFile, item.id, "Marked complete", "Status changed to DONE, completion 100%")
        t.println(green("✔ Marked ${item.id} as complete"))
    }

    private fun markCancelled(item: RoadmapItem) {
        RoadmapWriter.updateField(roadmapFile, item.id, "status", "CANCELLED")
        RoadmapWriter.appendChangelog(roadmapFile, item.id, "Marked cancelled", "Status changed to CANCELLED")
        t.println(yellow("✔ Marked ${item.id} as cancelled"))
    }

    private fun generateShortDescription(name: String, description: String, criticality: String): String {
        // Template-based generation
        val action = when {
            name.contains("migrate", ignoreCase = true) -> "Migrate"
            name.contains("implement", ignoreCase = true) -> "Implement"
            name.contains("add", ignoreCase = true) -> "Add"
            name.contains("fix", ignoreCase = true) -> "Fix"
            name.contains("update", ignoreCase = true) -> "Update"
            name.contains("investigate", ignoreCase = true) -> "Investigate"
            name.contains("enrich", ignoreCase = true) -> "Enrich"
            name.contains("plugin", ignoreCase = true) -> "Enable"
            name.contains("backend", ignoreCase = true) -> "Expose"
            else -> "Address"
        }
        // Take first few words of name as the core subject
        val coreSubject = name.split(" ").take(3).joinToString(" ")
        return "$action $coreSubject to address $criticality concern"
    }

    private fun calculateAge(creationDate: String): String {
        if (creationDate.isBlank()) return "Unknown"
        return try {
            val created = LocalDate.parse(creationDate, dateFormat)
            val days = java.time.temporal.ChronoUnit.DAYS.between(created, LocalDate.now())
            "${days} d"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun complexityLabel(level: Int): String {
        return when (level) {
            1 -> "Trivial"
            2 -> "Simple"
            3 -> "Moderate"
            4 -> "Complex"
            5 -> "Critical"
            else -> "Unknown"
        }
    }
}

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

        // 1. Check for roadmap mode
        if (argsList.contains("--roadmap") || argsList.firstOrNull() == "roadmap") {
            RoadmapApp.run()
            return
        }

        // 2. Check for proposals review mode
        if (argsList.contains("--proposals") || argsList.firstOrNull() == "proposals") {
            val autoYes = argsList.contains("-y") || argsList.contains("--yes")
            runProposalsMode(autoYes = autoYes)
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
                        val prop = proposals.find { it.proposalId.startsWith(targetId) }
                        if (prop != null) {
                            prop.enabled = !prop.enabled
                            renderProposalsList()
                        } else {
                            System.err.println(red("Proposal not found: $targetId"))
                        }
                    } else {
                        // Toggle first or prompt
                        System.err.println(yellow("Usage: t <id-prefix> to toggle selection (or 'all' / 'none')"))
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
