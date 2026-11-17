package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.firestore.SetOptions
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Command to restore or import tracker applications and collections from JSON, NDJSON, or backup files.
 */
public class ImportCommand : CliktCommand(name = "import") {
    private val filePath: String? by argument("file", help = "Path to backup file (JSON or NDJSON)").optional()
    private val fileOption: String? by option("--file", "-f", help = "Path to backup file (JSON or NDJSON)")
    private val dryRun: Boolean by option("--dry-run", help = "Validate and preview without writing").flag(default = false)

    override fun run() {
        val t = Terminal()
        val path = filePath ?: fileOption

        if (path.isNullOrBlank()) {
            t.println(red("Error: input file path is required. Usage: tracker import <file> or --file <file>"))
            return
        }

        val file = File(path)
        if (!file.exists()) {
            t.println(red("Error: file not found at ${file.absolutePath}"))
            return
        }

        if (!dryRun) {
            RemoteDataSourceProvider.assertWritesAllowed("tracker database restore/import")
        }

        t.println(cyan("Reading backup payload from ${file.name}..."))
        val lines = file.readLines().filter { it.isNotBlank() }

        val records = mutableListOf<Map<String, Any?>>()

        // Detect if NDJSON (multiple lines with valid JSON objects) or single JSON payload
        val firstLine = lines.firstOrNull() ?: ""
        if (lines.size > 1 && firstLine.startsWith("{")) {
            for (line in lines) {
                try {
                    val obj = Json.parseToJsonElement(line).jsonObject
                    val map = mutableMapOf<String, Any?>()
                    obj.forEach { (k, v) ->
                        map[k] = v.jsonPrimitive.contentOrNull
                    }
                    records.add(map)
                } catch (e: Exception) {
                    t.println(yellow("Warning: skipping invalid JSON line: ${e.message}"))
                }
            }
        } else {
            val fullText = file.readText()
            try {
                val element = Json.parseToJsonElement(fullText)
                if (element is JsonObject && element.containsKey("applications")) {
                    val appsArray = element["applications"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())
                    for (elem in appsArray) {
                        val map = mutableMapOf<String, Any?>()
                        elem.jsonObject.forEach { (k, v) ->
                            map[k] = v.jsonPrimitive.contentOrNull
                        }
                        records.add(map)
                    }
                } else if (element is JsonObject) {
                    val map = mutableMapOf<String, Any?>()
                    element.jsonObject.forEach { (k, v) ->
                        map[k] = v.jsonPrimitive.contentOrNull
                    }
                    records.add(map)
                }
            } catch (e: Exception) {
                t.println(red("Error parsing JSON payload: ${e.message}"))
                return
            }
        }

        t.println("Parsed ${records.size} record(s) from backup.")

        if (dryRun) {
            t.println(green("✓ [dry-run] Validated ${records.size} application record(s). No writes performed."))
            return
        }

        val db = RemoteDataSourceProvider.remote
        var count = 0
        for (rec in records) {
            val id = rec["id"]?.toString() ?: continue
            val docRef = db.collection("applications").document(id)
            docRef.set(rec, SetOptions.merge()).get()
            count++
        }

        t.println(green("✓ Successfully imported and restored $count application record(s) into Firestore."))
    }
}
