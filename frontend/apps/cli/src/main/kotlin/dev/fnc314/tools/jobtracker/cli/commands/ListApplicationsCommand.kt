package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

public class ListApplicationsCommand : CliktCommand(name = "list") {
    private val status: String? by option("--status", help = "Filter by status")
    private val openOnly: Boolean by option("--open", help = "Only open applications").flag(default = false)
    private val limit: Int? by option("--limit", help = "Max results to return").int()
    private val jsonOutput: Boolean by option("--json", help = "Output as JSON").flag(default = false)

    override fun run() {
        val t = Terminal()
        val db = FirestoreProvider.firestore
        var query = db.collection("applications") as com.google.cloud.firestore.Query

        if (status != null) {
            query = query.whereEqualTo("status", status)
        }
        if (openOnly) {
            query = query.whereEqualTo("isOpen", true)
        }
        if (limit != null && limit!! > 0) {
            query = query.limit(limit!!)
        }

        val snapshot = query.get().get()
        val docs = snapshot.documents.map { doc ->
            val data = doc.data
            data["id"] = doc.id
            data
        }

        if (jsonOutput) {
            val jsonList = docs.map { doc ->
                JsonObject(doc.mapValues { (_, v) -> JsonPrimitive(v?.toString()) })
            }
            t.println(Json.encodeToString(jsonList))
            return
        }

        if (docs.isEmpty()) {
            t.println(gray("No applications found matching criteria."))
            return
        }

        val tbl = table {
            header {
                row(cyan("ID"), cyan("Company"), cyan("Role"), cyan("Status"), cyan("Fit"), cyan("Emails"))
            }
            body {
                docs.forEach { doc ->
                    val id = doc["id"]?.toString() ?: ""
                    val company = doc["company"]?.toString() ?: ""
                    val role = doc["role"]?.toString() ?: ""
                    val docStatus = doc["status"]?.toString() ?: ""
                    val fit = doc["fitRating"]?.toString() ?: "-"
                    val emailList = (doc["emailMessageIds"] as? List<*>) ?: emptyList<Any>()
                    val emailsBadge = if (emailList.isNotEmpty()) {
                        green("${emailList.size} linked")
                    } else {
                        gray("-")
                    }

                    val statusFormatted = when (docStatus.uppercase()) {
                        "APPLIED", "OFFER", "HIRED" -> green(docStatus)
                        "INTERVIEW" -> yellow(docStatus)
                        else -> docStatus
                    }

                    row(id, company, role, statusFormatted, fit, emailsBadge)
                }
            }
        }
        t.println(tbl)
    }
}
