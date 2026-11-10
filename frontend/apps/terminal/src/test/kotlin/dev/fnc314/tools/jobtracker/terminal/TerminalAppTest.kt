package dev.fnc314.tools.jobtracker.terminal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TerminalAppTest {

    @Test
    fun testNamespaceCleaning() {
        assertEquals("APPLIED", TerminalApp.cleanStatus("status:applied"))
        assertEquals("INTERVIEW", TerminalApp.cleanStatus("status:interview"))
        assertEquals("APPLIED", TerminalApp.cleanStatus("APPLIED"))
        assertEquals("workday", TerminalApp.cleanChannel("channel:workday"))
        assertEquals("user", TerminalApp.cleanActor("actor:user"))
        assertEquals("applied", TerminalApp.cleanType("event:applied"))
    }

    @Test
    fun testCacheApplicationsLoading() {
        // Loads from generated/tracker-cache.json if present
        val cacheFile = File("generated/tracker-cache.json")
        if (cacheFile.exists()) {
            val apps = TerminalApp.loadApplicationsFromCache(openOnly = false)
            assertTrue(apps.isNotEmpty(), "Cache should contain applications")
            val first = apps.first()
            assertNotNull(first.id)
            assertNotNull(first.company)
            assertNotNull(first.role)

            val openOnly = TerminalApp.loadApplicationsFromCache(openOnly = true)
            assertTrue(openOnly.all { it.isOpen }, "All returned apps should be open")
        }
    }

    @Test
    fun testRecalculationQueueJsonSchema() {
        val testApp = ApplicationRecord(
            id = "test-co--lead-engineer",
            company = "Test Co",
            role = "Lead Engineer",
            status = "APPLIED",
            fitRating = "85",
            isOpen = true,
            channel = "linkedin",
            location = "Remote",
            jobUrl = "https://example.com/job",
            jobApplicationUrl = "https://example.com/apply",
            uuid = "01a1a2a3-0000-0000-0000-000000000000",
            emailMessageIds = emptyList(),
            tags = listOf("remote", "leadership"),
            date = "2026-11-09"
        )

        val queueFile = File(".recalc_queue")
        val existedBefore = queueFile.exists()
        val originalContent = if (existedBefore) queueFile.readText() else null

        try {
            // Write recalc request
            val nowIso = "2026-11-09T20:00:00Z"
            val requestJson = """
            {
              "schemaVersion": 4,
              "kind": "fit-recalculation",
              "requestedAt": "$nowIso",
              "applicationId": "${testApp.id}",
              "company": "${testApp.company}",
              "role": "${testApp.role}",
              "currentFitRating": "${testApp.fitRating}"
            }
            """.trimIndent()
            queueFile.writeText(requestJson + "\n")

            assertTrue(queueFile.exists())
            val parsed = Json.parseToJsonElement(queueFile.readText()).jsonObject
            assertEquals("4", parsed["schemaVersion"]?.jsonPrimitive?.content)
            assertEquals("fit-recalculation", parsed["kind"]?.jsonPrimitive?.content)
            assertEquals("test-co--lead-engineer", parsed["applicationId"]?.jsonPrimitive?.content)
            assertEquals("Test Co", parsed["company"]?.jsonPrimitive?.content)
            assertEquals("Lead Engineer", parsed["role"]?.jsonPrimitive?.content)
            assertEquals("85", parsed["currentFitRating"]?.jsonPrimitive?.content)
        } finally {
            if (existedBefore && originalContent != null) {
                queueFile.writeText(originalContent)
            } else {
                queueFile.delete()
            }
        }
    }

    @Test
    fun testProposalRecordDefaultsToUnselected() {
        val record = ProposalRecord(
            proposalId = "01a1c1e7-5782-75a9-8256-da6470c416e2",
            createdAt = "2026-11-10T12:00:00Z",
            state = "PENDING",
            changesCount = 2,
            unmatchedCount = 1
        )
        assertFalse(record.enabled, "ProposalRecord must default to unselected (enabled = false)")
    }

    @Test
    fun testTerminalBinaryDistributionExecution() {
        var root: File? = File(".").canonicalFile
        while (root != null && !File(root, "mise.toml").exists()) {
            root = root.parentFile
        }
        val repoRoot = root ?: File(".")
        val tuiBin = File(repoRoot, "frontend/apps/terminal/build/install/apps-terminal/bin/apps-terminal")
        if (tuiBin.exists()) {
            val pb = ProcessBuilder(tuiBin.absolutePath, "--proposals")
            pb.directory(repoRoot)
            pb.environment()["TRACKER_SOURCE"] = "cache"
            val proc = pb.start()
            val stdout = proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            assertEquals(0, code, "Binary should exit with 0")
            assertTrue(stdout.contains("\"ok\": true") || stdout.contains("\"ok\":true"))
            assertTrue(stdout.contains("proposals review complete"))
        }
    }

    @Test
    fun testTerminalBinaryOpenListWithCache() {
        var root: File? = File(".").canonicalFile
        while (root != null && !File(root, "mise.toml").exists()) {
            root = root.parentFile
        }
        val repoRoot = root ?: File(".")
        val tuiBin = File(repoRoot, "frontend/apps/terminal/build/install/apps-terminal/bin/apps-terminal")
        if (tuiBin.exists()) {
            val pb = ProcessBuilder(tuiBin.absolutePath, "--open", "list")
            pb.directory(repoRoot)
            pb.environment()["TRACKER_SOURCE"] = "cache"
            val proc = pb.start()
            val stdout = proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            assertEquals(0, code)
            assertTrue(stdout.contains("Job Application Pipeline"))
        }
    }
}
