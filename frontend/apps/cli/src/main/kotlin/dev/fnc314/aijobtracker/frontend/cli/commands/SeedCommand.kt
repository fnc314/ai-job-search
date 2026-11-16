package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

internal class SeedCommand : CliktCommand(name = "seed") {
    override fun run() {
        val t = Terminal()
        RemoteDataSourceProvider.assertWritesAllowed("seeding schema metadata")

        val schemaFile = File("tracker.schema.json")
        if (!schemaFile.exists()) {
            t.println(red("tracker.schema.json not found in working directory."))
            return
        }

        val parsed = Json.parseToJsonElement(schemaFile.readText()).jsonObject
        val version = parsed["schemaVersion"]?.jsonPrimitive?.content?.toIntOrNull() ?: 9

        val db = RemoteDataSourceProvider.remote
        val metaSchema = db.collection("meta").document("schema")
        val data = mapOf(
            "schemaVersion" to version,
            "seededAt" to Timestamp.now(),
            "source" to "frontend/apps/cli"
        )
        metaSchema.set(data).get()

        t.println(green("✓ Successfully seeded meta/schema with schemaVersion = $version"))
    }
}
