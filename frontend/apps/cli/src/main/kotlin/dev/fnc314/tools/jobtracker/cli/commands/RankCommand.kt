package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.tools.jobtracker.ranking.RankScorer
import dev.fnc314.tools.jobtracker.ranking.ScoringWeights
import kotlinx.serialization.json.Json
import java.io.File

/**
 * CLI command for evaluating a job posting against the candidate profile.
 *
 * Uses the deterministic RankScorer from `backend/job-post-ranking/` with keyword matching,
 * role-level detection, sector alignment, location scoring, calibration rules, and deal-breaker penalties.
 * Accepts a job description via argument, file path, or stdin, and outputs a markdown or JSON fit evaluation summary.
 */
public class RankCommand : CliktCommand(name = "rank") {
    private val urlOrJd: String by argument(
        "url-or-jd",
        help = "Job URL, local file path, or job description text",
    )
    private val jsonOutput: Boolean by option(
        "--json",
        help = "Output as JSON instead of markdown",
    ).flag()
    private val weightsJson: String? by option(
        "--weights",
        help = "Custom ScoringWeights as JSON (e.g. '{\"technical\": 0.40, \"experience\": 0.20, \"behavioral\": 0.10, \"career\": 0.30}')",
    )

    override fun run() {
        val t = Terminal()

        val jobText = when {
            urlOrJd.startsWith("http") -> {
                t.println(red("URL fetching not supported in Kotlin version. Use a file path or provide text directly."))
                return
            }
            else -> {
                val f = File(urlOrJd)
                if (f.exists() && f.isFile) {
                    f.readText()
                } else if ("/" in urlOrJd || "\\" in urlOrJd || File(urlOrJd).extension.isNotEmpty()) {
                    t.println(red("File not found: $urlOrJd"))
                    return
                } else {
                    // Treat as raw text
                    urlOrJd
                }
            }
        }

        if (jobText.isBlank()) {
            t.println(red("Job description is empty."))
            return
        }

        val weights = weightsJson?.let {
            runCatching {
                Json { ignoreUnknownKeys = true }.decodeFromString<ScoringWeights>(it)
            }.getOrNull() ?: ScoringWeights.DEFAULT
        } ?: ScoringWeights.DEFAULT

        val result = RankScorer.evaluate(jobText, weights)

        if (jsonOutput) {
            t.println(RankScorer.formatJson(result, trackerRecord = null))
        } else {
            t.println(RankScorer.formatMarkdown(result, trackerRecord = null))
        }
    }
}
