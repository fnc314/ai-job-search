package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import com.google.cloud.firestore.SetOptions
import dev.fnc314.tools.jobsearch.parser.DocumentIdGenerator
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

public class AddApplicationCommand : CliktCommand(name = "add") {
    private val jsonInput: String by option("--json", help = "JSON string or path to JSON file containing application data").required()

    override fun run() {
        val t = Terminal()
        FirestoreProvider.assertWritesAllowed("application creation")

        val rawContent = if (File(jsonInput).exists()) {
            File(jsonInput).readText()
        } else {
            jsonInput
        }

        try {
            val json = Json.parseToJsonElement(rawContent).jsonObject
            val id = json["id"]?.jsonPrimitive?.contentOrNull
                ?: throw IllegalArgumentException("Missing required 'id' field in JSON payload.")
            val company = json["company"]?.jsonPrimitive?.contentOrNull
                ?: throw IllegalArgumentException("Missing required 'company' field in JSON payload.")
            val role = json["role"]?.jsonPrimitive?.contentOrNull ?: ""
            val status = json["status"]?.jsonPrimitive?.contentOrNull ?: "APPLIED"

            val db = FirestoreProvider.firestore
            val docRef = db.collection("applications").document(id)
            val snap = docRef.get().get()

            if (snap.exists()) {
                t.println(red("Application already exists: $id"))
                return
            }

            val now = Timestamp.now()
            val companyId = DocumentIdGenerator.slug(company, fallback = "unknown-company")
            val docData = mutableMapOf<String, Any>(
                "id" to id,
                "company" to company,
                "companyId" to companyId,
                "role" to role,
                "status" to status,
                "isOpen" to true,
                "uuid" to Uuid7.generate().toString(),
                "schemaVersion" to 9,
                "emailMessageIds" to emptyList<String>(),
                "createdAt" to now,
                "updatedAt" to now
            )

            json["jobUrl"]?.jsonPrimitive?.contentOrNull?.let { docData["jobUrl"] = it }
            json["jobApplicationUrl"]?.jsonPrimitive?.contentOrNull?.let { docData["jobApplicationUrl"] = it }
            json["location"]?.jsonPrimitive?.contentOrNull?.let { docData["location"] = it }
            json["channel"]?.jsonPrimitive?.contentOrNull?.let { docData["channel"] = it }

            docRef.set(docData).get()

            // Upsert canonical company entry so companies collection stays synchronized
            db.collection("companies").document(companyId).set(
                mapOf(
                    "companyId" to companyId,
                    "name" to company,
                    "updatedAt" to now,
                ),
                SetOptions.merge(),
            ).get()

            // Initial event
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
        } catch (e: Exception) {
            t.println(red("Failed to add application: ${e.message}"))
            throw e
        }
    }
}
