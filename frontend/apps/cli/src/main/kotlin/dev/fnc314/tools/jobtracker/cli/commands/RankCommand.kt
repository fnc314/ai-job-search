package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.tools.jobsearch.parser.RankScorer
import java.io.File

/**
 * CLI command for evaluating a job posting against the candidate profile.
 *
 * Mirrors the Python `rank.py` script. Accepts a job description via argument,
 * file path, or stdin, and outputs a markdown or JSON fit evaluation summary.
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

        val result = RankScorer.evaluate(jobText)

        if (jsonOutput) {
            t.println(RankScorer.formatJson(result, trackerRecord = null))
        } else {
            t.println(RankScorer.formatMarkdown(result, trackerRecord = null))
        }
    }
}
