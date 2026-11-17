package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import com.google.cloud.firestore.SetOptions
import dev.fnc314.aijobtracker.records.parser.DocumentIdGenerator
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import dev.fnc314.aijobtracker.frontend.core.data.model.Uuid7
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

internal class UpsertCommand : CliktCommand(name = "upsert") {
    private val jsonInput: String by option("--json", help = "JSON string or path to JSON file containing application data").required()
    private val dryRun: Boolean by option("--dry-run", help = "Resolve and validate only without writing").flag()

    override fun run() {
        val t = Terminal()
        if (!dryRun) {
            RemoteDataSourceProvider.assertWritesAllowed("application upsert")
        }

        val rawContent = if (File(jsonInput).exists()) {
            File(jsonInput).readText()
        } else {
            jsonInput
        }

        try {
            val json = Json.parseToJsonElement(rawContent).jsonObject
            val company = json["company"]?.jsonPrimitive?.contentOrNull
                ?: throw IllegalArgumentException("Missing required 'company' field in JSON payload.")
            val role = json["role"]?.jsonPrimitive?.contentOrNull ?: ""
            val id = json["id"]?.jsonPrimitive?.contentOrNull
                ?: DocumentIdGenerator.documentId(company, role)
            val status = json["status"]?.jsonPrimitive?.contentOrNull ?: "APPLIED"

            val db = RemoteDataSourceProvider.remote
            val docRef = db.collection("applications").document(id)
            val snap = docRef.get().get()

            val now = Timestamp.now()
            val companyId = DocumentIdGenerator.slug(company, fallback = "unknown-company")

            if (dryRun) {
                t.println(yellow("[Dry Run] Would upsert application '$id' (exists: ${snap.exists()})"))
                return
            }

            val docData = mutableMapOf<String, Any>(
                "id" to id,
                "company" to company,
                "companyId" to companyId,
                "role" to role,
                "status" to status,
                "isOpen" to true,
                "updatedAt" to now
            )

            if (!snap.exists()) {
                docData["uuid"] = Uuid7.generate().toString()
                docData["schemaVersion"] = 9
                docData["emailMessageIds"] = emptyList<String>()
                docData["createdAt"] = now
            }

            json["jobUrl"]?.jsonPrimitive?.contentOrNull?.let { docData["jobUrl"] = it }
            json["jobApplicationUrl"]?.jsonPrimitive?.contentOrNull?.let { docData["jobApplicationUrl"] = it }
            json["location"]?.jsonPrimitive?.contentOrNull?.let { docData["location"] = it }
            json["channel"]?.jsonPrimitive?.contentOrNull?.let { docData["channel"] = it }

            docRef.set(docData, SetOptions.merge()).get()

            db.collection("companies").document(companyId).set(
                mapOf(
                    "companyId" to companyId,
                    "name" to company,
                    "updatedAt" to now,
                ),
                SetOptions.merge(),
            ).get()

            if (!snap.exists()) {
                val eventDoc = docRef.collection("events").document()
                val eventData = mapOf<String, Any>(
                    "eventId" to eventDoc.id,
                    "ts" to now,
                    "eventType" to "APPLY",
                    "actor" to "USER",
                    "statusTo" to status
                )
                eventDoc.set(eventData).get()
                t.println(green("✓ Successfully created application: $id ($company - $role)"))
            } else {
                t.println(green("✓ Successfully updated application: $id ($company - $role)"))
            }
        } catch (e: Exception) {
            t.println(red("Failed to upsert application: ${e.message}"))
            throw e
        }
    }
}
