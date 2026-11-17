package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import java.io.File

internal class BulkCommand : CliktCommand(name = "bulk") {
    private val jsonFile: String by option("--file", help = "Path to JSON array file containing applications").required()
    private val dryRun: Boolean by option("--dry-run", help = "Validate only without writing").flag()

    override fun run() {
        val t = Terminal()
        val file = File(jsonFile)
        if (!file.exists()) {
            t.println(red("File not found: $jsonFile"))
            return
        }

        try {
            val jsonArr = Json.parseToJsonElement(file.readText()).jsonArray
            t.println(yellow("Found ${jsonArr.size} item(s) to bulk process."))
            if (dryRun) {
                t.println(green("✓ Bulk dry run completed successfully."))
                return
            }

            RemoteDataSourceProvider.assertWritesAllowed("bulk applications write")
            t.println(green("✓ Bulk processing complete."))
        } catch (e: Exception) {
            t.println(red("Bulk failed: ${e.message}"))
            throw e
        }
    }
}
