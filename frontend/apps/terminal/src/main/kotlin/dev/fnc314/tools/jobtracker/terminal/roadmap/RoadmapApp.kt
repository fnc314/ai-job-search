package dev.fnc314.tools.jobtracker.terminal.roadmap

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
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.RawModeScope
import com.github.ajalt.mordant.input.enterRawMode
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Panel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Scanner
import java.util.concurrent.TimeUnit

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
            "shortdesc" -> "Short Desc"
            "complexity" -> "Complexity"
            "dependencies" -> "Dependencies"
            else -> field
        }

        // Multi-line fields: replace the entire field block (from **Field:** to the next **Field: or section marker)
        val multiLineFields = listOf("Objective", "Scope", "Acceptance Criteria", "Notes", "Dependencies")
        
        val updatedBlock = if (fieldKey in multiLineFields) {
            // For multi-line fields, find the field marker and replace everything up to the next marker
            val fieldMarker = "**$fieldKey:**"
            val markerPos = itemBlock.indexOf(fieldMarker)
            if (markerPos == -1) {
                // Field doesn't exist yet, add it before **Linked Plans:**
                val linkedPlansPos = itemBlock.indexOf("**Linked Plans:**")
                if (linkedPlansPos == -1) {
                    System.err.println(yellow("⚠ Could not find field '$fieldKey' or insertion point in $itemId"))
                    return false
                }
                val before = itemBlock.substring(0, linkedPlansPos)
                val after = itemBlock.substring(linkedPlansPos)
                val newField = before.trimEnd() + "\n" + fieldMarker + " " + value.trim() + "\n\n" + after
                newField
            } else {
                // Find where this field's content ends (next **Field: or blank line followed by non-content)
                val contentStart = markerPos + fieldMarker.length
                val remaining = itemBlock.substring(contentStart)
                
                // Find the end: next line starting with ** or a blank line followed by **
                val lines = remaining.lines()
                var contentEnd = 0 // characters consumed from remaining
                var blankCount = 0
                for (i in lines.indices) {
                    val line = lines[i]
                    if (line.startsWith("**")) {
                        break
                    }
                    if (line.isBlank()) {
                        blankCount++
                        if (blankCount >= 2) {
                            break
                        }
                    } else {
                        blankCount = 0
                    }
                    contentEnd = i + 1
                }
                
                var afterContentStart = contentStart
                for (j in 0 until contentEnd) {
                    afterContentStart += lines[j].length + 1 // +1 for newline
                }
                
                val before = itemBlock.substring(0, contentStart)
                val after = itemBlock.substring(afterContentStart)
                before.trimEnd() + " " + value.trim() + "\n" + after
            }
        } else {
            // Single-line field replacement
            val fieldPattern = Regex("(^\\*\\*$fieldKey:\\*\\*\\s+).*$", RegexOption.MULTILINE)
            itemBlock.replace(fieldPattern, "$1$value")
        }

        if (updatedBlock == itemBlock) {
            System.err.println(yellow("⚠ Could not find field '$fieldKey' in $itemId"))
            return false
        }

        val updated = content.substring(0, itemStart) + updatedBlock + content.substring(endBound)
        roadmapFile.writeText(updated)

        // Append changelog entry
        appendChangelog(roadmapFile, itemId, field, value.take(80) + if (value.length > 80) "..." else "")
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
    private var selectedIndex: Int = 0
    private var rawMode: RawModeScope? = null
    
    // Fallback raw mode: when Mordant's native interfaces (FFM/JNA) fail,
    // use `stty` to set raw mode and manual escape sequence parsing
    private var useFallbackRawMode = false
    private var fallbackSttyRestore: ProcessBuilder? = null

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

    public fun run(args: Array<String>) {
        if (!roadmapFile.exists()) {
            t.println(red("ROADMAP.md not found. Nothing to manage."))
            return
        }

        var items = RoadmapParser.parse(roadmapFile, plansReadme.takeIf { it.exists() })
        var currentView: String = "list" // list, detail, add
        selectedItem = null
        selectedIndex = 0

        // Parse command-line args for targeted editing: i RM-XXX or e RM-XXX
        if (args.size >= 2) {
            val cmd = args[0].lowercase()
            val itemId = args[1]
            if (cmd == "i" || cmd == "e") {
                val found = items.find { it.id.lowercase() == itemId.lowercase() }
                if (found != null) {
                    selectedItem = found
                    selectedIndex = items.indexOf(found)
                    currentView = "detail"
                } else {
                    t.println(red("Item not found: $itemId"))
                    return
                }
            }
        }

        // Close Scanner before entering raw mode to avoid input buffering conflicts
        scanner.close()

        // Enter full-screen mode with raw input
        val termWidth = getTerminalWidth()
        print("\u001b[2J\u001b[H") // Clear screen
        print("\u001b[?25l") // Hide cursor

        // Try Mordant's native raw mode first; fall back to stty-based raw mode
        try {
            rawMode = t.enterRawMode()
        } catch (e: Exception) {
            // Mordant's native interfaces (FFM/JNA) failed — use stty fallback
            useFallbackRawMode = true
            try {
                ProcessBuilder("stty", "-f", "/dev/tty", "-echo", "-icanon", "-isig", "min", "1", "time", "0")
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            } catch (_: Exception) {
                t.println(red("Failed to enter raw mode. Arrow keys may not work."))
            }
            // Save restore command for cleanup
            fallbackSttyRestore = ProcessBuilder("stty", "-f", "/dev/tty", "echo", "icanon", "isig")
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
        }
        try {
            while (true) {
                // Clear and redraw for full-screen effect
                print("\u001b[2J\u001b[H")

                // Render header bar
                renderFullScreenHeader(currentView, items.size, termWidth)

                when (currentView) {
                    "list" -> {
                        val next = renderListView(items)
                        if (next == "exit") {
                            print("\u001b[?25h") // Show cursor
                            t.println()
                            t.println(dim("Goodbye!"))
                            return
                        }
                        currentView = next
                    }
                    "detail" -> {
                        if (selectedItem != null) {
                            val next = renderDetailView(selectedItem!!, items)
                            if (next == "exit") {
                                print("\u001b[?25h") // Show cursor
                                t.println()
                                t.println(dim("Goodbye!"))
                                return
                            }
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
                    "exit" -> {
                        print("\u001b[?25h") // Show cursor
                        t.println()
                        t.println(dim("Goodbye!"))
                        return
                    }
                }

                // Render status bar
                renderFullScreenStatusBar(currentView, termWidth)

                System.out.flush()
            }
        } finally {
            print("\u001b[?25h") // Show cursor
            rawMode?.close()
            rawMode = null
            // Restore terminal if we used stty fallback
            if (useFallbackRawMode) {
                try {
                    fallbackSttyRestore?.start()?.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                } catch (_: Exception) {
                    // ignore restore errors
                }
            }
        }
    }

    // Render the full-screen header bar
    private fun renderFullScreenHeader(currentView: String, itemCount: Int, width: Int) {
        val viewLabel = when (currentView) {
            "list" -> "List"
            "detail" -> "Detail"
            "add" -> "Add"
            else -> "Unknown"
        }
        val title = bold(cyan("★ Roadmap Manager ★"))
        val info = dim("Items: $itemCount | View: $viewLabel")
        val barContent = "$title $info"
        t.println(barContent.padEnd(width, '─'))
        t.println()
    }

    // Render the full-screen status bar at the bottom
    private fun renderFullScreenStatusBar(currentView: String, width: Int) {
        t.println()
        val hints = when (currentView) {
            "list" -> dim("↑/↓ navigate • Enter inspect • + add • clear freeze • q quit • Tab complete ID")
            "detail" -> dim("e: show edit menu • s/c/p/k/m/t edit • o/g/a/n/l multiline • t: ↑/↓ ±1% Shift±10% Opt±5% • b back")
            "add" -> dim("Fill fields • Esc cancel")
            else -> dim("")
        }
        t.println("─".repeat(width.coerceAtMost(80)))
        t.println(hints)
    }

    // Read a single key press using the active raw mode context.
    // Returns: "up", "down", "left", "right", "shift_up", "shift_down", "shift_left", "shift_right",
    // "opt_up", "opt_down", "opt_left", "opt_right", "enter", "tab", "backspace", "delete", "escape", "ctrl_d",
    // or the raw character.
    private fun readKey(): String {
        // If Mordant raw mode failed, use fallback escape sequence parser
        if (useFallbackRawMode) {
            return readKeyFallback()
        }
        val rm = rawMode ?: return "enter" // fallback if raw mode not active
        val event: KeyboardEvent = rm.readKey()
        if (event.isCtrlC) return "ctrl_c" // Ctrl+C exits the TUI
        val key = event.key
        return when {
            key == "ArrowUp" -> {
                if (event.shift) "shift_up"
                else if (event.alt) "opt_up"
                else "up"
            }
            key == "ArrowDown" -> {
                if (event.shift) "shift_down"
                else if (event.alt) "opt_down"
                else "down"
            }
            key == "ArrowRight" -> {
                if (event.shift) "shift_right"
                else if (event.alt) "opt_right"
                else "right"
            }
            key == "ArrowLeft" -> {
                if (event.shift) "shift_left"
                else if (event.alt) "opt_left"
                else "left"
            }
            key == "Enter" -> "enter"
            key == "Tab" -> "tab"
            key == "Backspace" -> "backspace"
            key == "Delete" -> "delete"
            key == "Escape" -> "escape"
            key == "d" && event.ctrl -> "ctrl_d"
            else -> key
        }
    }

    // Fallback key reader using raw System.in.read() when Mordant's native interfaces fail.
    // Parses escape sequences for arrow keys, Tab, Backspace, Delete, Ctrl shortcuts,
    // and SHIFT/OPTION modifiers.
    private fun readKeyFallback(): String {
        val c1 = readRawByte() ?: return "enter"
        if (c1 == 27) { // ESC
            val c2 = readRawByte() ?: return "escape"
            if (c2 == 91) { // [
                val c3 = readRawByte() ?: return "escape"
                return when (c3) {
                    65 -> "up"       // A — plain Up
                    66 -> "down"     // B — plain Down
                    67 -> "right"    // C — plain Right
                    68 -> "left"     // D — plain Left
                    49 -> { // '1' — could be modifier sequence or Delete
                        val c4 = readRawByte() ?: return "escape"
                        if (c4 == 126) return "delete" // ~ — Delete key
                        if (c4 == 59) { // ; — modifier sequence
                            val c5 = readRawByte() ?: return "escape"
                            val c6 = readRawByte() ?: return "escape"
                            val modifier = when (c5) {
                                50 -> "shift_"
                                51 -> "opt_"
                                52 -> "ctrl_"
                                53 -> "shift_opt_"
                                else -> ""
                            }
                            val arrow = when (c6) {
                                65 -> "up"
                                66 -> "down"
                                67 -> "right"
                                68 -> "left"
                                else -> String(charArrayOf(c6.toChar()))
                            }
                            modifier + arrow
                        } else {
                            String(charArrayOf(c3.toChar(), c4.toChar()))
                        }
                    }
                    51 -> { // '3' — Delete key alternate
                        val c4 = readRawByte() ?: return "escape"
                        if (c4 == 126) return "delete"
                        String(charArrayOf(c3.toChar(), c4.toChar()))
                    }
                    90 -> "tab"      // Z — Shift+Tab (treat as Tab)
                    else -> String(charArrayOf(c1.toChar(), c2.toChar(), c3.toChar()))
                }
            }
            return "escape" // ESC alone
        }
        if (c1 == 13) return "enter" // \r
        if (c1 == 10) return "enter" // \n
        if (c1 == 9) return "tab"    // Tab
        if (c1 == 127) return "backspace" // Backspace (Del)
        if (c1 == 8) return "backspace"   // Backspace (BS)
        if (c1 == 4) return "ctrl_d"      // Ctrl+D
        if (c1 == 3) return "ctrl_c"      // Ctrl+C
        return String(charArrayOf(c1.toChar()))
    }

    // Read a single byte from stdin (blocking). With stty min 1 time 0, read() blocks
    // until at least one byte is available. Falls back to polling if -1 is returned.
    private fun readRawByte(): Int? {
        var attempts = 0
        while (true) {
            val b = try {
                System.`in`.read()
            } catch (_: Exception) {
                -1
            }
            if (b >= 0) return b
            // EOF or error — brief pause and retry (should not happen with min 1)
            attempts++
            if (attempts > 50) return null // ~1 second timeout
            try { Thread.sleep(20) } catch (_: InterruptedException) { return null }
        }
    }

    private fun renderListView(items: List<RoadmapItem>): String {
        // Clamp selectedIndex to valid range
        if (selectedIndex < 0) selectedIndex = 0
        if (items.isNotEmpty() && selectedIndex >= items.size) selectedIndex = items.size - 1

        fun renderTable() {
            t.println()
            t.println(t.render(
                Panel(
                    content = bold(brightBlue("Roadmap Manager")) + "\n" +
                        dim("↑/↓ navigate • Enter inspect • + add • clear freeze all • q quit"),
                    title = cyan("★ Roadmap ★")
                )
            ))

            t.println()
            t.println(cyan("ACTIVE ITEMS") + white(" | ") + bold("${items.size} items"))
            t.println()

            if (items.isEmpty()) {
                t.println(gray("No active roadmap items. Press + to add one."))
                t.println()
                return
            }

            val tbl = table {
                header {
                    row(cyan("ID"), cyan("Name"), cyan("Short Desc"), cyan("Criticality"), cyan("Age"))
                }
                body {
                    items.forEachIndexed { idx, item ->
                        val age = calculateAge(item.creationDate)
                        val isSelected = idx == selectedIndex
                        val critColor = when (item.criticality.uppercase()) {
                            "PIVOTAL" -> bold(red(item.criticality))
                            "HIGH" -> yellow(item.criticality)
                            "MEDIUM" -> item.criticality
                            "LOW" -> gray(item.criticality)
                            else -> item.criticality
                        }
                        val prefix = if (isSelected) bold(yellow("▸ ")) else "  "
                        val idCell = if (isSelected) bold(yellow(item.id)) else item.id
                        val titleCell = if (isSelected) bold(yellow(item.title)) else item.title
                        val descCell = if (isSelected) bold(yellow(item.shortDesc)) else item.shortDesc
                        val critCell = if (isSelected) bold(yellow(critColor)) else critColor
                        val ageCell = if (isSelected) bold(yellow(age)) else age
                        row(prefix + idCell, titleCell, descCell, critCell, ageCell)
                    }
                }
            }
            t.println(t.render(tbl))
        }

        if (items.isEmpty()) {
            renderTable()
            t.println()
            t.print(brightBlue("roadmap [+ add, q quit] > "))
            val line = readSingleLine()
            return when {
                line == "exit" -> "exit"
                line == "+" || line == "add" -> "add"
                line == "q" || line == "quit" -> "exit"
                else -> "list"
            }
        }

        // Main input loop for arrow navigation
        while (true) {
            renderTable()
            t.println()
            t.print(brightBlue("roadmap [↑/↓ navigate, Enter inspect, + add, id, clear, q] > "))

            val key = readKey()

            when {
                key == "up" -> {
                    if (selectedIndex > 0) selectedIndex--
                }
                key == "down" -> {
                    if (selectedIndex < items.size - 1) selectedIndex++
                }
                key == "enter" -> {
                    if (items.isNotEmpty()) {
                        selectedItem = items[selectedIndex]
                        return "detail"
                    }
                }
                key == "q" || key == "Q" -> return "exit"
                key == "ctrl_c" -> return "exit"
                key == "+" -> return "add"
                key == "c" -> {
                    // Check if user types "clear"
                    val rest = buildString {
                        append(key)
                        repeat(4) {
                            val c = readKey()
                            if (c.length == 1) append(c)
                            else {
                                // Not "clear", fall through to normal handling
                                return@buildString
                            }
                        }
                    }
                    if (rest == "clear") {
                        handleClearRoadmap(items)
                    }
                    // If not "clear", treat first char as unknown and re-render
                }
                else -> {
                    // Could be the start of typing an ID — read the full line with Tab completion
                    val line = buildString {
                        append(key)
                        // Read until enter, with Tab completion support
                        while (true) {
                            val k = readKey()
                            if (k == "enter") break
                            if (k == "tab") {
                                // Auto-complete to the first matching ID
                                val partial = this.toString().lowercase()
                                val match = items.find { it.id.lowercase().startsWith(partial) && it.id.lowercase() != partial }
                                if (match != null) {
                                    // Replace current input with the matched ID
                                    clear()
                                    append(match.id)
                                    t.println()
                                    t.print(brightBlue("roadmap [↑/↓ navigate, Enter inspect, + add, id, clear, q] > ") + match.id)
                                }
                                continue
                            }
                            if (k.length == 1) append(k)
                            else break
                        }
                    }.trim()

                    if (line.isEmpty()) return "list"

                    return when {
                        line == "q" || line == "quit" -> "exit"
                        line == "+" || line == "add" -> "add"
                        line == "clear" -> {
                            handleClearRoadmap(items)
                            "list"
                        }
                        items.any { it.id.lowercase() == line.lowercase() || it.id == line } -> {
                            selectedItem = items.find { it.id.lowercase() == line.lowercase() || it.id == line }
                            selectedIndex = items.indexOf(selectedItem)
                            "detail"
                        }
                        else -> {
                            t.println(red("Unknown item: $line"))
                            "list"
                        }
                    }
                }
            }
        }
    }

    private fun renderDetailView(item: RoadmapItem, items: List<RoadmapItem>): String {
        t.println()
        val isFrozen = item.status.uppercase() == "FROZEN"
        val statusColor = when (item.status.uppercase()) {
            "DONE" -> green(item.status)
            "IN_PROGRESS" -> yellow(item.status)
            "OPEN" -> cyan(item.status)
            "CANCELLED" -> gray(item.status)
            "FROZEN" -> dim(item.status)
            else -> item.status
        }

        // Frozen item warning
        if (isFrozen) {
            t.println(t.render(
                Panel(content = red(bold("⚠ FROZEN ITEM — No edits allowed. Press 'b' to go back.")),
                    title = red("⛔ Read Only"))
            ))
        }

        val ro = gray(" [read-only]")
        val content = buildString {
            appendLine("ID:            ${bold(item.id)}$ro")
            appendLine("Title:         ${bold(item.title)}$ro")
            appendLine("Short Desc:    ${item.shortDesc}")
            appendLine("Criticality:   ${item.criticality}")
            appendLine("Priority:      ${item.priority}")
            appendLine("Complexity:    ${item.complexity} — ${complexityLabel(item.complexity)}")
            appendLine("Status:        $statusColor")
            appendLine("Completion:    ${item.completionPercent}%")
            appendLine("Created:       ${item.creationDate}$ro")
            appendLine("Age:           ${calculateAge(item.creationDate)}$ro")
            appendLine()
            appendLine(bold("Objective:"))
            appendLine(item.objective)
            if (item.scope.isNotBlank()) {
                appendLine()
                appendLine(bold("Scope:"))
                appendLine(item.scope)
            }
            if (item.dependencies.isNotBlank() && item.dependencies != "None") {
                appendLine()
                appendLine(bold("Dependencies:"))
                appendLine(item.dependencies)
            }
            if (item.acceptanceCriteria.isNotBlank()) {
                appendLine()
                appendLine(bold("Acceptance Criteria:"))
                appendLine(item.acceptanceCriteria)
            }
            if (item.notes.isNotBlank()) {
                appendLine()
                appendLine(bold("Notes:"))
                appendLine(item.notes)
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

        // Frozen items: only allow back
        if (isFrozen) {
            t.println()
            t.print(brightBlue("${item.id} [b:back] > "))
            val line = readSingleLine()
            return when {
                line == "exit" -> "exit"  // Ctrl+C propagates up to main loop
                line == "b" || line == "back" -> "back"
                else -> {
                    t.println(red("Frozen items cannot be modified. Press 'b' to go back."))
                    "detail"
                }
            }
        }

        t.println()
        t.println(yellow("Edit commands (press 'e' to re-show this list):"))
        t.println("  s  - Short Description")
        t.println("  c  - Criticality")
        t.println("  p  - Priority")
        t.println("  k  - Complexity (0-5)")
        t.println("  m  - Status (menu: OPEN, IN_PROGRESS, DONE, CANCELLED)")
        t.println("  t  - Completion % (↑/↓ ±1, Shift ±10, Opt ±5, type number)")
        t.println("  o  - Objective (@file to import)")
        t.println("  g  - Scope (@file to import)")
        t.println("  a  - Acceptance Criteria (@file to import)")
        t.println("  n  - Notes (@file to import)")
        t.println("  l  - Dependencies (@file to import)")
        t.println(gray("  (ID, Title, Created, Age are read-only)"))
        t.println("  b  - Back to list")
        t.print(brightBlue("${item.id} > "))
        val line = readSingleLine()

        return when {
            line == "exit" -> "exit"  // Ctrl+C propagates up to main loop
            line == "b" || line == "back" -> "back"
            line == "e" || line == "edit" -> "detail"
            line == "c" || line == "criticality" -> {
                updateCriticality(item)
                "updated"
            }
            line == "p" || line == "priority" -> {
                updatePriority(item)
                "updated"
            }
            line == "k" || line == "complexity" -> {
                updateComplexity(item)
                "updated"
            }
            line == "s" || line == "shortdesc" -> {
                updateShortDesc(item)
                "updated"
            }
            line == "m" || line == "status" -> {
                updateStatus(item)
                "updated"
            }
            line == "t" || line == "completion" -> {
                updateCompletion(item)
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
            line == "o" || line == "objective" -> {
                updateMultiLineField(item, "Objective", "objective")
                "updated"
            }
            line == "g" || line == "scope" -> {
                updateMultiLineField(item, "Scope", "scope")
                "updated"
            }
            line == "a" || line == "acceptance" -> {
                updateMultiLineField(item, "Acceptance Criteria", "acceptanceCriteria")
                "updated"
            }
            line == "n" || line == "notes" -> {
                updateMultiLineField(item, "Notes", "notes")
                "updated"
            }
            line == "l" || line == "dependencies" -> {
                updateMultiLineField(item, "Dependencies", "dependencies")
                "updated"
            }
            else -> {
                t.println(red("Unknown command. See edit commands above."))
                "detail"
            }
        }
    }

    private fun updateStatus(item: RoadmapItem) {
        val options = listOf("OPEN", "IN_PROGRESS", "DONE", "CANCELLED")
        val labels = listOf("1=OPEN", "2=IN_PROGRESS", "3=DONE", "4=CANCELLED")
        updateEnumFieldWithAlias(item, "Status", "Status", options, item.status, labels)
    }

    // Enum field editor that also accepts numeric aliases (e.g., "1" for "OPEN")
    private fun updateEnumFieldWithAlias(
        item: RoadmapItem,
        fieldKey: String,
        fieldName: String,
        options: List<String>,
        currentValue: String,
        labels: List<String>
    ) {
        t.println()
        t.println("Current $fieldName: ${bold(currentValue)}")
        t.println("Options: ${labels.joinToString(", ")}")
        t.println(yellow("Press Tab to cycle through options"))

        var currentIndex = if (options.contains(currentValue.uppercase())) options.indexOf(currentValue.uppercase()) else -1

        while (true) {
            t.print(brightBlue("New $fieldName (Tab to cycle): "))

            val input = readInputLineWithTab(options, currentIndex) { selectedIdx ->
                currentIndex = selectedIdx
                options[selectedIdx]
            }

            when {
                input == "exit" -> return  // Ctrl+C
                input == "escape" -> {
                    t.println(yellow("Cancelled."))
                    return
                }
                input.isEmpty() -> {
                    t.println(yellow("Empty value. Cancelled."))
                    return
                }
                input.uppercase() in options -> {
                    val status = input.uppercase()
                    RoadmapWriter.updateField(roadmapFile, item.id, fieldKey, status)
                    RoadmapWriter.appendChangelog(roadmapFile, item.id, "$fieldName changed", "$fieldName: $currentValue → $status")
                    t.println(green("✔ Updated $fieldName to $status"))
                    return
                }
                else -> {
                    t.println(red("Invalid $fieldName. Options: ${labels.joinToString(", ")}"))
                }
            }
        }
    }

    private fun updateCompletion(item: RoadmapItem) {
        t.println()
        t.println("Current completion: ${bold(cyan("${item.completionPercent}%"))}")
        t.println("Note: Setting 100% will auto-mark status as DONE")
        t.println()
        t.println("  ↑/↓ : ±1%  |  Shift+↑/↓ : ±10%  |  Opt+↑/↓ : ±5%")
        t.println("  Type number and press Enter for exact value")
        t.println("  Esc to cancel")
        t.println()

        var value = item.completionPercent

        while (true) {
            // Display current value prominently
            t.print(brightBlue("Completion %: ") + cyan(bold(String.format("%3d%%", value))) + " > ")

            val key = readKey()
            when {
                key == "up" -> {
                    value = (value + 1).coerceIn(0, 100)
                    t.println(cyan(bold(String.format("%3d%%", value))))
                }
                key == "down" -> {
                    value = (value - 1).coerceIn(0, 100)
                    t.println(cyan(bold(String.format("%3d%%", value))))
                }
                key == "shift_up" -> {
                    value = (value + 10).coerceIn(0, 100)
                    t.println(yellow(bold(String.format("%3d%% (+10)", value))))
                }
                key == "shift_down" -> {
                    value = (value - 10).coerceIn(0, 100)
                    t.println(yellow(bold(String.format("%3d%% (-10)", value))))
                }
                key == "opt_up" -> {
                    value = (value + 5).coerceIn(0, 100)
                    t.println(green(bold(String.format("%3d%% (+5)", value))))
                }
                key == "opt_down" -> {
                    value = (value - 5).coerceIn(0, 100)
                    t.println(green(bold(String.format("%3d%% (-5)", value))))
                }
                key == "enter" -> {
                    // Save the current value
                    saveCompletion(item, value, item.completionPercent)
                    return
                }
                key == "escape" -> {
                    t.println(yellow("Cancelled."))
                    return
                }
                key == "ctrl_c" -> {
                    return  // exit the editor, back to detail view
                }
                key.length == 1 && key.toIntOrNull() != null -> {
                    // Direct numeric input — read the full number (digits only)
                    val rest = readNumericLine()
                    if (rest == "exit") return  // Ctrl+C during numeric input
                    val fullInput = key + rest
                    val num = fullInput.toIntOrNull()
                    if (num != null && num in 0..100) {
                        value = num
                        t.println(cyan(bold(String.format("%3d%%", value))))
                    } else if (num != null) {
                        t.println(red("Invalid. Must be 0-100."))
                    } else {
                        t.println(red("Invalid percentage. Must be 0-100."))
                    }
                }
                key.length == 1 -> {
                    // Reject non-numeric characters immediately
                    t.println()
                    t.println(red("Invalid input: '$key' — only digits, arrow keys, or Enter accepted."))
                }
                else -> {
                    // ignore special keys
                }
            }
        }
    }

    private fun saveCompletion(item: RoadmapItem, newValue: Int, oldValue: Int) {
        RoadmapWriter.updateField(roadmapFile, item.id, "Completion %", "$newValue")
        if (newValue == 100) {
            RoadmapWriter.updateField(roadmapFile, item.id, "Status", "DONE")
            RoadmapWriter.appendChangelog(roadmapFile, item.id, "Completed", "Completion reached 100%, status auto-set to DONE")
            t.println(green("✔ Completion set to 100%, status auto-marked as DONE"))
        } else {
            RoadmapWriter.appendChangelog(roadmapFile, item.id, "Completion updated", "Completion: ${oldValue}% → ${newValue}%")
            t.println(green("✔ Updated completion to ${newValue}%"))
        }
    }

    private fun updateComplexity(item: RoadmapItem) {
        val options = listOf("0 — Unknown", "1 — Trivial", "2 — Simple", "3 — Moderate", "4 — Complex", "5 — Critical")
        t.println()
        t.println("Current complexity: ${item.complexity} — ${complexityLabel(item.complexity)}")
        t.println("Options: ${options.joinToString(", ")}")
        t.println(yellow("Press Tab to cycle through options"))

        var currentIndex = if (item.complexity in 0..5) item.complexity else 0

        while (true) {
            t.print(brightBlue("New Complexity (0-5, Tab to cycle): "))
            val key = readKey()
            when {
                key == "tab" -> {
                    currentIndex = (currentIndex + 1) % 6
                    val label = complexityLabel(currentIndex)
                    t.println()
                    t.print(brightBlue("New Complexity (0-5, Tab to cycle): " + cyan(bold("$currentIndex — $label"))))
                }
                key == "enter" -> {
                    val label = complexityLabel(currentIndex)
                    RoadmapWriter.updateField(roadmapFile, item.id, "Complexity", "$currentIndex — $label")
                    t.println()
                    t.println(green("✔ Updated complexity to $currentIndex — $label"))
                    return
                }
                key == "escape" -> {
                    t.println(yellow("Cancelled."))
                    return
                }
                key == "ctrl_c" -> {
                    return  // exit the editor
                }
                key.length == 1 -> {
                    val num = key.toIntOrNull()
                    if (num != null && num in 0..5) {
                        val label = complexityLabel(num)
                        RoadmapWriter.updateField(roadmapFile, item.id, "Complexity", "$num — $label")
                        t.println()
                        t.println(green("✔ Updated complexity to $num — $label"))
                        return
                    } else if (key.matches(Regex("[a-zA-Z]"))) {
                        t.println(red("Invalid: letters not allowed. Must be a digit 0-5."))
                    } else {
                        t.println(red("Invalid complexity. Must be 0-5."))
                    }
                }
                else -> {
                    // ignore
                }
            }
        }
    }

    private fun updateShortDesc(item: RoadmapItem) {
        t.println()
        t.println("Current: ${item.shortDesc}")

        while (true) {
            t.print(brightBlue("New Short Description (Esc to cancel): "))
            val value = readSingleLine()
            when {
                value == "exit" -> return  // Ctrl+C
                value == "escape" -> {
                    t.println(yellow("Cancelled."))
                    return
                }
                value.isBlank() -> {
                    t.println(red("Short description cannot be empty."))
                }
                else -> {
                    RoadmapWriter.updateField(roadmapFile, item.id, "Short Desc", value)
                    t.println(green("✔ Updated Short Desc"))
                    return
                }
            }
        }
    }

    // Read a single line of input using readKey() with Backspace and Escape support
    // Returns the typed string, "escape" if cancelled, "exit" on Ctrl+C, or empty string on EOF
    private fun readSingleLine(): String {
        val typed = StringBuilder()
        while (true) {
            val key = readKey()
            when {
                key == "enter" -> return typed.toString().trim()
                key == "escape" -> return "escape"
                key == "ctrl_c" -> return "exit"
                key == "backspace" -> {
                    if (typed.length > 0) {
                        typed.deleteAt(typed.length - 1)
                        t.print("\b \b")
                    }
                }
                key.length == 1 -> {
                    typed.append(key)
                    t.print(key)
                }
                else -> {
                    // ignore unknown keys
                }
            }
        }
    }

    // Read a numeric-only line of input — rejects letters as they are typed.
    // Returns the typed string (digits only), "escape" if cancelled, "exit" on Ctrl+C, or empty string on EOF.
    private fun readNumericLine(): String {
        val typed = StringBuilder()
        while (true) {
            val key = readKey()
            when {
                key == "enter" -> return typed.toString().trim()
                key == "escape" -> return "escape"
                key == "ctrl_c" -> return "exit"
                key == "backspace" -> {
                    if (typed.length > 0) {
                        typed.deleteAt(typed.length - 1)
                        t.print("\b \b")
                    }
                }
                key.length == 1 && key.toIntOrNull() != null -> {
                    typed.append(key)
                    t.print(key)
                }
                key.length == 1 -> {
                    // Reject non-numeric characters immediately
                    t.println()
                    t.println(red("Invalid: '$key' is not a digit. Only numbers accepted."))
                    t.print(brightBlue("Value: "))
                }
                else -> {
                    // ignore unknown keys
                }
            }
        }
    }

    private fun updateMultiLineField(item: RoadmapItem, fieldKey: String, fieldName: String) {
        t.println()
        t.println("Current $fieldKey:")
        val currentValue = when (fieldName) {
            "objective" -> item.objective
            "scope" -> item.scope
            "acceptanceCriteria" -> item.acceptanceCriteria
            "notes" -> item.notes
            "dependencies" -> item.dependencies
            else -> ""
        }
        if (currentValue.isNotBlank() && currentValue != "None") {
            t.println(dim("---"))
            t.println(currentValue)
            t.println(dim("---"))
        }
        t.println()
        t.println(yellow("Options:"))
        t.println("  Type content directly (↑/↓ navigate lines, ←/→ cursor, Esc cancel, Ctrl+D finish)")
        t.println("  Or start with @<path> to import from a file")
        t.println("  Or type END on first line to clear")
        t.println()

        // Read first line to check for file import or END
        t.print(brightBlue("> "))
        val firstKey = readKey()
        
        when {
            firstKey == "escape" -> {
                t.println(yellow("Cancelled."))
                return
            }
            firstKey == "ctrl_d" -> {
                t.println(yellow("Empty content. Cancelled."))
                return
            }
            firstKey.length == 1 -> {
                val firstLineStart = firstKey
                
                if (firstLineStart == "@") {
                    // File import mode — read the rest of the path
                    val pathRest = readSingleLine()
                    val filePath = (firstLineStart + pathRest).substring(1).trim()
                    val file = if (filePath.startsWith("/")) {
                        File(filePath)
                    } else {
                        File(repoRoot, filePath)
                    }
                    if (!file.exists()) {
                        t.println(red("File not found: $filePath"))
                        return
                    }
                    val content = file.readText()
                    val lineCount = content.lines().size
                    t.println()
                    t.println(green("Found file: $filePath ($lineCount lines, ${content.length} chars)"))
                    t.print(brightBlue("Import? [y/N]: "))
                    val confirmKey = readKey()
                    t.println()
                    if (confirmKey.lowercase() == "y") {
                        RoadmapWriter.updateField(roadmapFile, item.id, fieldKey, content)
                        t.println(green("✔ Imported $fieldKey from $filePath"))
                    } else {
                        t.println(yellow("Cancelled."))
                    }
                    return
                }
                
                // Check if the user typed "END" immediately
                val restOfLine = readSingleLine()
                val firstLine = firstLineStart + restOfLine
                
                if (firstLine.trim().uppercase() == "END") {
                    t.println(yellow("Empty content. Cancelled."))
                    return
                }
                
                // Multi-line editor mode
                val lines = mutableListOf(firstLine)
                val result = readMultiLineEditor(lines)
                
                when {
                    result == null -> {
                        t.println(yellow("Cancelled."))
                        return
                    }
                    result.isBlank() -> {
                        t.println(yellow("Empty content. Cancelled."))
                        return
                    }
                    else -> {
                        RoadmapWriter.updateField(roadmapFile, item.id, fieldKey, result)
                        t.println(green("✔ Updated $fieldKey (${result.lines().size} lines)"))
                    }
                }
            }
            else -> {
                // Arrow key or other special key — start with empty buffer
                val lines = mutableListOf("")
                val result = readMultiLineEditor(lines)
                
                when {
                    result == null -> {
                        t.println(yellow("Cancelled."))
                        return
                    }
                    result.isBlank() -> {
                        t.println(yellow("Empty content. Cancelled."))
                        return
                    }
                    else -> {
                        RoadmapWriter.updateField(roadmapFile, item.id, fieldKey, result)
                        t.println(green("✔ Updated $fieldKey (${result.lines().size} lines)"))
                    }
                }
            }
        }
    }

    // Multi-line editor with 2D keyboard navigation
    // Returns the final content as a string, or null if cancelled
    private fun readMultiLineEditor(initialLines: MutableList<String>): String? {
        val lines = initialLines.toMutableList()
        var cursorLine = 0
        var cursorCol = 0.coerceAtMost(lines[0].length)
        
        // Terminal dimensions (fallback to 80x24)
        val termWidth = getTerminalWidth()
        val termHeight = getTerminalHeight()
        
        // Clear screen and enter full-screen mode
        print("\u001b[2J\u001b[H") // Clear screen and home cursor
        print("\u001b[?25l") // Hide cursor
        
        try {
            while (true) {
                // Render the editor
                renderMultiLineEditor(lines, cursorLine, cursorCol, termWidth, termHeight)
                
                val key = readKey()
                when {
                    key == "ctrl_d" -> {
                        // Finish and return
                        return lines.joinToString("\n")
                    }
                    key == "escape" -> {
                        return null
                    }
                    key == "enter" -> {
                        // Insert new line after current position
                        val before = lines[cursorLine].substring(0, cursorCol)
                        val after = lines[cursorLine].substring(cursorCol)
                        lines[cursorLine] = before
                        lines.add(cursorLine + 1, after)
                        cursorLine++
                        cursorCol = 0
                    }
                    key == "backspace" -> {
                        if (cursorCol > 0) {
                            // Delete character before cursor
                            val line = lines[cursorLine]
                            lines[cursorLine] = line.substring(0, cursorCol - 1) + line.substring(cursorCol)
                            cursorCol--
                        } else if (cursorLine > 0) {
                            // Join with previous line
                            val currentLine = lines[cursorLine]
                            lines.removeAt(cursorLine)
                            lines[cursorLine - 1] = lines[cursorLine - 1] + currentLine
                            cursorLine--
                            cursorCol = lines[cursorLine].length
                        }
                    }
                    key == "delete" -> {
                        val line = lines[cursorLine]
                        if (cursorCol < line.length) {
                            lines[cursorLine] = line.substring(0, cursorCol) + line.substring(cursorCol + 1)
                        } else if (cursorLine < lines.size - 1) {
                            // Delete next line (join with following)
                            val nextLine = lines[cursorLine + 1]
                            lines[cursorLine] = lines[cursorLine] + nextLine
                            lines.removeAt(cursorLine + 1)
                        }
                    }
                    key == "up" -> {
                        if (cursorLine > 0) {
                            cursorLine--
                            cursorCol = cursorCol.coerceAtMost(lines[cursorLine].length)
                        }
                    }
                    key == "down" -> {
                        if (cursorLine < lines.size - 1) {
                            cursorLine++
                            cursorCol = cursorCol.coerceAtMost(lines[cursorLine].length)
                        }
                    }
                    key == "left" -> {
                        if (cursorCol > 0) {
                            cursorCol--
                        }
                    }
                    key == "right" -> {
                        val line = lines[cursorLine]
                        if (cursorCol < line.length) {
                            cursorCol++
                        }
                    }
                    key.length == 1 -> {
                        // Insert character at cursor position
                        val line = lines[cursorLine]
                        lines[cursorLine] = line.substring(0, cursorCol) + key + line.substring(cursorCol)
                        cursorCol++
                    }
                }
                
                // Ensure cursor bounds
                if (cursorLine >= lines.size) cursorLine = lines.size - 1
                if (cursorLine < 0) cursorLine = 0
                if (cursorCol > lines[cursorLine].length) cursorCol = lines[cursorLine].length
                if (cursorCol < 0) cursorCol = 0
            }
        } finally {
            print("\u001b[?25h") // Show cursor
        }
    }

    // Render the multi-line editor view
    private fun renderMultiLineEditor(
        lines: List<String>,
        cursorLine: Int,
        cursorCol: Int,
        termWidth: Int,
        termHeight: Int
    ) {
        print("\u001b[2J\u001b[H") // Clear screen
        
        // Header
        val header = Panel(
            content = bold(brightBlue("Multi-line Editor")) + "\n" +
                dim("↑/↓ move between lines • ←/→ cursor • Backspace/Delete edit"),
            title = cyan("★ Edit ★")
        )
        val headerText = t.render(header).trim()
        t.println(headerText)
        t.println()
        
        // Calculate visible area
        val availableHeight = termHeight - 6 // Reserve space for header and footer
        val availableWidth = termWidth - 2
        
        // Determine visible window
        val viewStart = maxOf(0, cursorLine - 3)
        val viewEnd = minOf(lines.size - 1, viewStart + availableHeight - 2)
        
        // Render lines with line numbers
        for (i in viewStart..viewEnd) {
            val lineNum = i + 1
            val isCursorLine = i == cursorLine
            val lineContent = lines[i]
            
            val numStr = "$lineNum:".padStart(4)
            val numColor = if (isCursorLine) cyan(bold(numStr)) else gray(numStr)
            
            if (isCursorLine) {
                // Show cursor position
                val beforeCursor = lineContent.substring(0, cursorCol.coerceAtMost(lineContent.length))
                val atCursor = if (cursorCol < lineContent.length) {
                    bold(yellow(lineContent[cursorCol].toString()))
                } else {
                    bold(yellow("▌"))
                }
                val afterCursor = lineContent.substring(cursorCol.coerceAtMost(lineContent.length))
                t.println("$numColor $beforeCursor$atCursor$afterCursor")
            } else {
                t.println("$numColor $lineContent")
            }
        }
        
        // Footer
        t.println()
        val footer = buildString {
            append(dim("Lines: ${lines.size} | Cursor: ${cursorLine + 1}:${cursorCol + 1}"))
            append(" | ")
            append(yellow("Ctrl+D: Save"))
            append(" | ")
            append(yellow("Esc: Cancel"))
        }
        t.println(footer)
        
        // Flush output
        System.out.flush()
    }

    // Get terminal width, defaulting to 80
    private fun getTerminalWidth(): Int {
        return try {
            val cols = System.getenv("COLUMNS")?.toIntOrNull()
            cols ?: 80
        } catch (_: Exception) {
            80
        }
    }

    // Get terminal height, defaulting to 24
    private fun getTerminalHeight(): Int {
        return try {
            val lines = System.getenv("LINES")?.toIntOrNull()
            lines ?: 24
        } catch (_: Exception) {
            24
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
        val name = readSingleLine()
        if (name == "exit" || name == "escape" || name.isBlank()) {
            t.println(yellow("Name is required. Cancelled."))
            return null
        }

        t.print(brightBlue("Description (long): "))
        val description = readSingleLine()
        if (description == "exit" || description == "escape" || description.isBlank()) {
            t.println(yellow("Description is required. Cancelled."))
            return null
        }

        t.println()
        t.println("Criticality options: PIVOTAL, HIGH, MEDIUM, LOW, NOT_SET")
        t.print(brightBlue("Criticality: "))
        val criticality = readSingleLine()
        if (criticality == "exit" || criticality == "escape") return null
        val criticalityUpper = criticality.trim().uppercase()
        if (criticalityUpper !in listOf("PIVOTAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) {
            t.println(yellow("Invalid criticality. Defaulting to NOT_SET."))
        }
        val finalCriticality = if (criticalityUpper in listOf("PIVOTAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) criticalityUpper else "NOT_SET"

        t.println()
        t.println("Priority options: CRITICAL, HIGH, MEDIUM, LOW, NOT_SET")
        t.print(brightBlue("Priority: "))
        val priority = readSingleLine()
        if (priority == "exit" || priority == "escape") return null
        val priorityUpper = priority.trim().uppercase()
        if (priorityUpper !in listOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) {
            t.println(yellow("Invalid priority. Defaulting to NOT_SET."))
        }
        val finalPriority = if (priorityUpper in listOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")) priorityUpper else "NOT_SET"

        // Generate short description
        t.println()
        t.println(yellow("Generating short description..."))
        val generatedShortDesc = generateShortDescription(name, description, finalCriticality)
        t.println()
        t.println(green("Generated: ") + generatedShortDesc)
        t.println()
        t.print(brightBlue("[a]ccept, [e]dit, [r]eplace: "))
        val shortDescChoice = readSingleLine()
        if (shortDescChoice == "exit" || shortDescChoice == "escape") return null

        val shortDesc = when (shortDescChoice.lowercase()) {
            "a", "accept" -> generatedShortDesc
            "e", "edit" -> {
                t.print(brightBlue("Edit short description: "))
                val edited = readSingleLine()
                if (edited == "exit" || edited == "escape") return null
                if (edited.isBlank()) generatedShortDesc else edited
            }
            "r", "replace" -> {
                t.print(brightBlue("Enter new short description: "))
                val replaced = readSingleLine()
                if (replaced == "exit" || replaced == "escape") return null
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
        val confirm = readSingleLine()

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
        if (readSingleLine().trim() != "CLEAR") {
            t.println(yellow("Cancelled."))
            return
        }

        // Confirmation 2
        t.print(brightBlue("Type 'CLEAR ROADMAP' to continue: "))
        if (readSingleLine().trim() != "CLEAR ROADMAP") {
            t.println(yellow("Cancelled."))
            return
        }

        // Confirmation 3
        t.print(brightBlue("Type 'CLEAR ALL' to confirm: "))
        if (readSingleLine().trim() != "CLEAR ALL") {
            t.println(yellow("Cancelled."))
            return
        }

        // Reason (min 50 chars)
        t.println()
        t.print(brightBlue("Provide a reason (minimum 50 characters): "))
        var reason = readSingleLine().trim()
        while (reason.length < 50) {
            t.println(yellow("Reason must be at least 50 characters (currently ${reason.length})."))
            t.print(brightBlue("Reason: "))
            reason = readSingleLine().trim()
        }

        RoadmapWriter.freezeAllItems(roadmapFile, reason)
        t.println()
        t.println(green(bold("✔ Roadmap frozen! All ${items.size} items have been archived.")))
        t.println(dim("Reason: $reason"))
    }

    private fun updateCriticality(item: RoadmapItem) {
        val options = listOf("PIVOTAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")
        updateEnumField(item, "criticality", "Criticality", options, item.criticality.uppercase())
    }

    private fun updatePriority(item: RoadmapItem) {
        val options = listOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "NOT_SET")
        updateEnumField(item, "priority", "Priority", options, item.priority.uppercase())
    }

    // Generic enum field editor with Tab cycling and form validation
    private fun updateEnumField(
        item: RoadmapItem,
        fieldKey: String,
        fieldName: String,
        options: List<String>,
        currentValue: String
    ) {
        t.println()
        t.println("Current $fieldName: ${bold(currentValue)}")
        t.println("Options: ${options.joinToString(", ")}")
        t.println(yellow("Press Tab to cycle through options"))

        var currentIndex = if (options.contains(currentValue)) options.indexOf(currentValue) else -1

        while (true) {
            t.print(brightBlue("New $fieldName (Tab to cycle): "))

            // Read raw input with Tab support
            val input = readInputLineWithTab(options, currentIndex) { selectedIdx ->
                currentIndex = selectedIdx
                options[selectedIdx]
            }

            when {
                input == "exit" -> return  // Ctrl+C
                input == "escape" -> {
                    t.println(yellow("Cancelled."))
                    return
                }
                input.isEmpty() -> {
                    t.println(yellow("Empty value. Cancelled."))
                    return
                }
                input.uppercase() in options -> {
                    RoadmapWriter.updateField(roadmapFile, item.id, fieldKey, input.uppercase())
                    t.println(green("✔ Updated $fieldName to ${input.uppercase()}"))
                    return
                }
                else -> {
                    t.println(red("Invalid $fieldName. Options: ${options.joinToString(", ")}"))
                }
            }
        }
    }

    // Read a line of input where Tab cycles through options
    // Returns the final selected value, "escape" if cancelled, "exit" on Ctrl+C, or empty string on EOF
    private fun readInputLineWithTab(
        options: List<String>,
        startIndex: Int,
        onCycle: (Int) -> String
    ): String {
        var cycleIndex = if (startIndex >= 0) startIndex else 0
        val typed = StringBuilder()

        while (true) {
            val key = readKey()
            when {
                key == "tab" -> {
                    // Cycle to next option
                    cycleIndex = (cycleIndex + 1) % options.size
                    val selected = onCycle(cycleIndex)
                    // Show the cycled value
                    t.println()
                    t.print(brightBlue("New value (Tab to cycle): " + cyan(bold(selected))))
                    typed.clear()
                    typed.append(selected)
                }
                key == "enter" -> {
                    return typed.toString().trim()
                }
                key == "escape" -> {
                    return "escape"
                }
                key == "ctrl_c" -> {
                    return "exit"
                }
                key == "backspace" -> {
                    if (typed.length > 0) {
                        typed.deleteAt(typed.length - 1)
                        t.print("\b \b")
                    }
                }
                key.length == 1 -> {
                    typed.append(key)
                    t.print(key)
                }
                else -> {
                    // Unknown key, ignore
                }
            }
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