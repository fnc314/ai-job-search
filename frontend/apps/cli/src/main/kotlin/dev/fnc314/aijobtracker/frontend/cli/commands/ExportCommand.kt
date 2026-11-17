package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

/**
 * Command to export tracker applications and data to stdout or a file in JSON, CSV, or NDJSON format.
 */
public class ExportCommand : CliktCommand(name = "export") {
    private val jsonOutput: Boolean by option("--json", help = "Full JSON payload (default)").flag(default = true)
    private val csvOutput: Boolean by option("--csv", help = "Legacy CSV shape").flag(default = false)
    private val ndjsonOutput: Boolean by option("--ndjson", help = "One application per line").flag(default = false)
    private val output: String? by option("-o", "--output", help = "Write to a file instead of stdout")

    override fun run() {
        val t = Terminal()
        val db = RemoteDataSourceProvider.remote

        val appsSnapshot = db.collection("applications").get().get()
        val apps = appsSnapshot.documents.map { doc ->
            val data = doc.data
            data["id"] = doc.id
            data
        }.sortedBy { (it["id"] ?: "").toString() }

        val text: String = when {
            csvOutput -> {
                val headers = listOf("id", "company", "role", "status", "date", "fitScore")
                val rows = mutableListOf(headers.joinToString(","))
                apps.forEach { app ->
                    val line = headers.map { h ->
                        val v = app[h]?.toString() ?: ""
                        if (v.contains(",") || v.contains("\"")) "\"${v.replace("\"", "\"\"")}\"" else v
                    }.joinToString(",")
                    rows.add(line)
                }
                rows.joinToString("\n") + "\n"
            }
            ndjsonOutput -> {
                apps.joinToString("\n") { app ->
                    val jsonObj = JsonObject(app.mapValues { (_, v) -> JsonPrimitive(v?.toString()) })
                    Json.encodeToString(jsonObj)
                } + "\n"
            }
            else -> {
                val jsonList = apps.map { app ->
                    JsonObject(app.mapValues { (_, v) -> JsonPrimitive(v?.toString()) })
                }
                val payload = JsonObject(mapOf("applications" to kotlinx.serialization.json.JsonArray(jsonList)))
                Json { prettyPrint = true }.encodeToString(payload) + "\n"
            }
        }

        if (output != null) {
            val file = File(output!!)
            file.parentFile?.mkdirs()
            file.writeText(text)
            t.println(green("✓ Successfully exported ${apps.size} application(s) to ${file.path}"))
        } else {
            print(text)
        }
    }
}
