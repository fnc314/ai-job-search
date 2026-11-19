package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.aijobtracker.backend.ai.client.AiRankingRequest
import dev.fnc314.aijobtracker.backend.routes.ApplyPipelineCoordinator
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * CLI command delegating directly to [ApplyPipelineCoordinator] so URL parsing, Hunter.io
 * enrichment, deterministic + AI fit ranking, and artifact generation share a single backend path.
 */
internal class ApplyCommand : CliktCommand(name = "apply") {
    private val target: String by argument("target", help = "Job URL, file path, or raw job description text")
    private val companyOverride: String? by option("--company", help = "Target company name override")
    private val roleOverride: String? by option("--role", help = "Target role title override")
    private val useAiRank: Boolean by option("--ai-rank", help = "Run Tier 2 AI semantic calibration")
        .flag("--no-ai-rank", default = true)
    private val draftAssets: Boolean by option("--draft-assets", help = "Generate tailored CV and cover letter sources")
        .flag("--no-draft-assets", default = false)
    private val markSubmitted: Boolean by option("--submitted", help = "Mark application as SUBMITTED")
        .flag("--no-submitted", default = false)
    private val allowWrites: Boolean by option("--persist", help = "Persist status transitions to Firestore")
        .flag(default = false)
    private val outputRoot: String by option("--output-dir", help = "Root directory for generated artifacts")
        .default("job-applications")

    override fun run() = runBlocking {
        val t = Terminal()
        if (allowWrites) {
            RemoteDataSourceProvider.assertWritesAllowed("apply pipeline")
        }

        val isUrl = target.startsWith("http://") || target.startsWith("https://")
        val targetFile = if (!isUrl) File(target).takeIf { it.exists() && it.isFile } else null
        val urlArg = if (isUrl) target else null
        val textArg = when {
            isUrl -> null
            targetFile != null -> targetFile.readText()
            else -> target
        }

        val deterministic = ApplyPipelineCoordinator.evaluateDeterministic(
            url = urlArg,
            rawText = textArg,
            companyOverride = companyOverride,
            roleOverride = roleOverride,
            providedApplicationId = null,
            persistToRemote = allowWrites,
        )

        if (deterministic.postingText.isBlank()) {
            t.println(red("Unable to extract posting text from target: $target"))
            return@runBlocking
        }

        t.println(
            cyan(
                "Tier 1 Deterministic Score (${deterministic.company} — ${deterministic.role}): " +
                    "${deterministic.score}/100 (${deterministic.verdict})"
            )
        )
        if (allowWrites && deterministic.persisted) {
            t.println(green("Firestore status updated: RANKED (${deterministic.applicationId})"))
        }

        if (useAiRank) {
            runCatching {
                ApplyPipelineCoordinator.evaluateWithAi(
                    request = AiRankingRequest(
                        postingText = deterministic.postingText,
                        company = deterministic.company,
                        title = deterministic.role,
                        deterministicScore = deterministic.score,
                        strengths = deterministic.strengths,
                        gaps = deterministic.gaps,
                        dealBreakers = deterministic.dealBreakers,
                    ),
                    applicationId = if (allowWrites) deterministic.applicationId else null,
                )
            }.onSuccess { calibrated ->
                t.println(green("Tier 2 AI Calibrated Score: ${calibrated.score}/100 (${calibrated.verdict})"))
                t.println("Rationale: ${calibrated.aiRationale}")
            }.onFailure { err ->
                t.println(yellow("Tier 2 AI calibration skipped: ${err.message}"))
            }
        }

        if (draftAssets) {
            val snapshot = ApplyPipelineCoordinator.executeArtifactJob(
                applicationId = deterministic.applicationId,
                company = deterministic.company,
                role = deterministic.role,
                postingText = deterministic.postingText,
                persistToRemote = allowWrites,
                outputRootDir = File(outputRoot),
            )
            if (snapshot.status == "SUCCESS") {
                t.println(
                    green(
                        "Drafted LaTeX & outcome artifacts: " +
                            listOfNotNull(snapshot.cvPdfUrl, snapshot.coverLetterPdfUrl, snapshot.outcomeUrl)
                                .joinToString(", ")
                    )
                )
                if (allowWrites) {
                    t.println(green("Firestore status updated: DRAFTED (${deterministic.applicationId})"))
                }
            } else {
                t.println(red("Artifact drafting failed: ${snapshot.error ?: "Unknown error"}"))
            }
        }

        if (markSubmitted && allowWrites) {
            ApplyPipelineCoordinator.markApplicationSubmitted(
                applicationId = deterministic.applicationId,
                note = "Application submitted via CLI apply pipeline",
            )
            t.println(green("Firestore status updated: SUBMITTED (${deterministic.applicationId})"))
        }
    }
}
