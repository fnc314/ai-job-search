package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import com.google.cloud.firestore.SetOptions
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.util.UUID

import com.github.ajalt.clikt.parameters.arguments.optional

/**
 * Top-level Clikt command managing proposals (`proposals list`, `proposals show`, `proposals approve`, `proposals reject`, `proposals expire`).
 */
public class ProposalCommand : CliktCommand(name = "proposals") {
    init {
        subcommands(
            ListProposalsCommand(),
            ShowProposalCommand(),
            ApproveProposalCommand(),
            RejectProposalCommand(),
            ExpireProposalsCommand(),
        )
    }

    override fun run(): Unit = Unit
}

internal class ListProposalsCommand : CliktCommand(name = "list") {
    private val state: String? by option("--state", help = "Filter by state (e.g. PENDING, APPROVED, REJECTED)")
    private val jsonOutput: Boolean by option("--json", help = "Output as JSON").flag(default = false)

    override fun run() {
        val t = Terminal(nonInteractiveWidth = 200)
        val db = RemoteDataSourceProvider.remote
        var query = db.collection("pending-proposals") as com.google.cloud.firestore.Query

        if (!state.isNullOrBlank()) {
            val normalizedState = state!!.removePrefix("proposal:").uppercase()
            query = query.whereEqualTo("state", normalizedState)
        }

        val snapshot = query.get().get()
        val docs = snapshot.documents.map { doc ->
            val data = doc.data
            data["proposalId"] = doc.id
            data
        }.sortedByDescending { (it["createdAt"] ?: it["collectedAt"])?.toString() ?: "" }

        if (jsonOutput) {
            val jsonList = docs.map { doc ->
                JsonObject(doc.mapValues { (_, v) -> JsonPrimitive(v?.toString()) })
            }
            t.println(Json.encodeToString(jsonList))
            return
        }

        if (docs.isEmpty()) {
            t.println(gray("No proposals found."))
            return
        }

        t.println(
            table {
                header {
                    row("Proposal ID", "State", "Changes", "Unmatched", "Created At", "Reviewed By")
                }
                body {
                    docs.forEach { doc ->
                        val pId = (doc["proposalId"] as? String)?.take(36) ?: "-"
                        val st = (doc["state"] as? String) ?: "PENDING"
                        val stFormatted = when (st.uppercase()) {
                            "PENDING" -> yellow(st)
                            "APPROVED" -> green(st)
                            "REJECTED" -> red(st)
                            else -> gray(st)
                        }
                        val changes = ((doc["changes"] as? List<*>)?.size ?: 0).toString()
                        val unmatched = ((doc["unmatched"] as? List<*>)?.size ?: 0).toString()
                        val createdAt = (doc["createdAt"] ?: doc["collectedAt"])?.toString()?.take(19) ?: "-"
                        val reviewedBy = (doc["reviewedBy"] as? String) ?: "-"

                        row(pId, stFormatted, changes, unmatched, createdAt, reviewedBy)
                    }
                }
            }
        )
    }
}

internal class ShowProposalCommand : CliktCommand(name = "show") {
    private val proposalId: String by argument("id", help = "Proposal ID")

    override fun run() {
        val t = Terminal(nonInteractiveWidth = 200)
        val db = RemoteDataSourceProvider.remote
        val snap = db.collection("pending-proposals").document(proposalId).get().get()

        if (!snap.exists()) {
            t.println(red("Proposal not found: $proposalId"))
            return
        }

        val data = snap.data ?: emptyMap<String, Any?>()
        val state = (data["state"] as? String) ?: "UNKNOWN"
        t.println(cyan("Proposal $proposalId [${state.uppercase()}]"))
        t.println(gray("Created: ${data["createdAt"] ?: data["collectedAt"]}, Reviewed: ${data["reviewedAt"] ?: "-"}, ReviewedBy: ${data["reviewedBy"] ?: "-"}"))

        val changes = data["changes"] as? List<Map<String, Any?>> ?: emptyList()
        t.println("\nProposed Changes (${changes.size}):")
        if (changes.isEmpty()) {
            t.println(gray("  None"))
        } else {
            changes.forEachIndexed { i, c ->
                val appId = (c["applicationId"] as? String) ?: c["discoveredCompany"] ?: "Unknown"
                val eventType = c["eventType"] ?: c["event"] ?: "STATUS_UPDATE"
                val sFrom = c["statusFrom"] ?: "-"
                val sTo = c["statusTo"] ?: "-"
                val conf = c["confidence"] ?: "MEDIUM_CONFIDENCE"
                val email = c["sourceEmail"] as? Map<String, Any?>
                val subject = email?.get("subject") ?: "-"
                t.println("  [${i + 1}] $appId: $eventType ($sFrom -> $sTo) [$conf]")
                t.println(gray("      Subject: $subject"))
            }
        }

        val unmatched = data["unmatched"] as? List<Map<String, Any?>> ?: emptyList()
        if (unmatched.isNotEmpty()) {
            t.println("\nUnmatched Messages (${unmatched.size}):")
            unmatched.take(10).forEach { u ->
                val sender = u["sender"] ?: "-"
                val subject = u["subject"] ?: "-"
                val reason = u["reason"] ?: "-"
                t.println("  - From: $sender | $subject | $reason")
            }
            if (unmatched.size > 10) {
                t.println(gray("  ... and ${unmatched.size - 10} more"))
            }
        }
    }
}

internal class ApproveProposalCommand : CliktCommand(name = "approve") {
    private val proposalId: String? by argument("id", help = "Proposal ID").optional()
    private val yes: Boolean by option("-y", "--yes", help = "Skip confirmation").flag(default = false)
    private val note: String? by option("--note", help = "Resolution note")
    private val only: String? by option("--only", help = "Comma-separated change indexes (1-based) to approve")

    override fun run() {
        val t = Terminal()
        RemoteDataSourceProvider.assertWritesAllowed("proposal approval")

        val targetId = proposalId ?: run {
            t.println(red("Proposal ID required."))
            return
        }

        val db = RemoteDataSourceProvider.remote
        val docRef = db.collection("pending-proposals").document(targetId)
        val snap = docRef.get().get()

        if (!snap.exists()) {
            t.println(red("Proposal not found: $targetId"))
            return
        }

        val data = snap.data ?: emptyMap<String, Any?>()
        val state = (data["state"] as? String) ?: "PENDING"
        if (state != "PENDING") {
            t.println(red("Proposal $targetId is already $state"))
            return
        }

        val selectedIndexes: Set<Int>? = only?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
        val changes = data["changes"] as? List<Map<String, Any?>> ?: emptyList()

        val approved = mutableListOf<Map<String, Any?>>()
        val skipped = mutableListOf<Map<String, Any?>>()

        for ((idx0, change) in changes.withIndex()) {
            val idx = idx0 + 1
            if (selectedIndexes != null && idx !in selectedIndexes) {
                skipped.add(mapOf("index" to idx, "reason" to "not selected"))
                continue
            }

            val appId = change["applicationId"] as? String
            val eventType = (change["eventType"] as? String) ?: (change["event"] as? String) ?: "ADD_NOTE"
            val statusTo = change["statusTo"] as? String
            val sourceEmail = change["sourceEmail"] as? Map<String, Any?>
            val messageId = sourceEmail?.get("messageId") as? String

            if (!appId.isNullOrBlank()) {
                val appRef = db.collection("applications").document(appId)
                val appSnap = appRef.get().get()
                if (appSnap.exists()) {
                    val appData = appSnap.data ?: emptyMap()
                    val currentStatus = appData["status"] as? String
                    if (statusTo != null && currentStatus == statusTo) {
                        skipped.add(mapOf("applicationId" to appId, "reason" to "already $currentStatus; $eventType would duplicate it"))
                        continue
                    }

                    // Append event to application
                    val eventDoc = appRef.collection("events").document()
                    val now = Timestamp.now()
                    val evData = mutableMapOf<String, Any?>(
                        "eventId" to eventDoc.id,
                        "ts" to now,
                        "date" to now.toDate().toInstant().toString().substringBefore("T"),
                        "eventType" to eventType,
                        "actor" to "EMAIL_SYNC",
                        "statusTo" to statusTo,
                        "note" to (sourceEmail?.get("subject") ?: note)
                    )
                    messageId?.let { evData["emailMessageId"] = it }
                    eventDoc.set(evData).get()

                    // Update parent status if statusTo provided
                    val updateMap = mutableMapOf<String, Any>("updatedAt" to now)
                    if (statusTo != null) {
                        updateMap["status"] = statusTo
                    }
                    appRef.set(updateMap, SetOptions.merge()).get()

                    approved.add(mapOf("applicationId" to appId, "eventId" to eventDoc.id, "type" to eventType))
                } else {
                    skipped.add(mapOf("applicationId" to appId, "reason" to "application record not found"))
                }
            } else {
                skipped.add(mapOf("index" to idx, "reason" to "unsupported lead or non-app change in cli runner"))
            }
        }

        val nowIso = Instant.now().toString()
        val resolution = "approved ${approved.size} change(s)"
        docRef.set(
            mapOf(
                "state" to "APPROVED",
                "reviewedAt" to nowIso,
                "reviewedBy" to "actor:user",
                "resolution" to (note ?: resolution)
            ),
            SetOptions.merge()
        ).get()

        t.println(green("✓ Successfully approved proposal $targetId ($resolution, ${skipped.size} skipped)"))
    }
}

internal class RejectProposalCommand : CliktCommand(name = "reject") {
    private val proposalId: String by argument("id", help = "Proposal ID")
    private val reason: String? by option("--reason", help = "Rejection reason")

    override fun run() {
        val t = Terminal()
        RemoteDataSourceProvider.assertWritesAllowed("proposal rejection")

        val db = RemoteDataSourceProvider.remote
        val docRef = db.collection("pending-proposals").document(proposalId)
        val snap = docRef.get().get()

        if (!snap.exists()) {
            t.println(red("Proposal not found: $proposalId"))
            return
        }

        val data = snap.data ?: emptyMap<String, Any?>()
        val state = (data["state"] as? String) ?: "PENDING"
        if (state != "PENDING") {
            t.println(red("Proposal $proposalId is already $state"))
            return
        }

        val nowIso = Instant.now().toString()
        val res = reason ?: "rejected by user"
        docRef.set(
            mapOf(
                "state" to "REJECTED",
                "reviewedAt" to nowIso,
                "reviewedBy" to "actor:user",
                "resolution" to res
            ),
            SetOptions.merge()
        ).get()

        t.println(green("✓ Rejected proposal $proposalId ($res)"))
    }
}

internal class ExpireProposalsCommand : CliktCommand(name = "expire") {
    override fun run() {
        val t = Terminal()
        RemoteDataSourceProvider.assertWritesAllowed("proposal expiration")

        val db = RemoteDataSourceProvider.remote
        val query = db.collection("pending-proposals").whereEqualTo("state", "PENDING")
        val snapshot = query.get().get()
        val now = Instant.now()
        var count = 0

        for (doc in snapshot.documents) {
            val dueAtStr = doc.getString("dueAt")
            if (dueAtStr != null) {
                try {
                    val dueAt = Instant.parse(dueAtStr)
                    if (dueAt.isBefore(now)) {
                        doc.reference.set(
                            mapOf(
                                "state" to "EXPIRED",
                                "reviewedAt" to now.toString(),
                                "reviewedBy" to "actor:system",
                                "resolution" to "expired past dueAt"
                            ),
                            SetOptions.merge()
                        ).get()
                        count++
                    }
                } catch (ignored: Exception) {}
            }
        }

        t.println(green("✓ Expired $count pending proposal(s)"))
    }
}
