package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.aijobtracker.records.parser.CompanySalaryBenchmark
import dev.fnc314.aijobtracker.records.parser.SalaryBenchmarkEngine
import dev.fnc314.aijobtracker.records.parser.SalaryDataset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Command for querying salary benchmark data from a local JSON dataset.
 */
public class SalaryCommand : CliktCommand(name = "salary") {
    private val company: String? by argument("company", help = "Company name to search for").optional()
    private val city: String? by option("--city", help = "Filter by city name")
    private val jsonOutput: Boolean by option("--json", help = "Output as JSON").flag(default = false)
    private val listAll: Boolean by option("--list-all", help = "List all companies").flag(default = false)
    private val validate: Boolean by option("--validate", help = "Validate salary data file and print a report").flag(default = false)

    override fun run() {
        val t = Terminal(nonInteractiveWidth = 200)

        // Resolve data file canonically
        val candidates = listOf(
            File("salary_data.json"),
            File("data/salary_benchmarks.json"),
            File("data/salary_data.json"),
            File("scripts/salary_data.json"),
        )
        val dataFile = candidates.firstOrNull { it.exists() }

        if (dataFile == null) {
            t.println(yellow("Note: No salary_data.json found. Salary lookup is optional and will be skipped."))
            return
        }

        val result = SalaryBenchmarkEngine.loadDataset(dataFile)
        if (result.isFailure) {
            t.println(red("Error: Failed to parse salary benchmark data: ${result.exceptionOrNull()?.message}"))
            return
        }

        val dataset = result.getOrThrow()

        if (validate) {
            t.println(green("✓ Successfully validated ${dataset.companies.size} company records in ${dataFile.path}"))
            dataset.metadata?.let { m ->
                t.println(gray("Source: ${m.source ?: "unknown"}, Baseline: ${m.indexBaseline ?: 100.0}"))
            }
            return
        }

        if (listAll) {
            if (jsonOutput) {
                t.println(Json.encodeToString(dataset.companies.map { it.company }))
            } else {
                t.println(cyan("All Companies in Salary Benchmark Database (${dataset.companies.size}):"))
                dataset.companies.sortedBy { it.company }.forEach { c ->
                    t.println(" - ${c.company}${if (c.city != null) " (${c.city})" else ""}")
                }
            }
            return
        }

        val targetCompany = company
        if (targetCompany.isNullOrBlank()) {
            t.println(yellow("Please specify a company name or use --list-all / --validate."))
            return
        }

        val matches = SalaryBenchmarkEngine.search(dataset, targetCompany, city)

        if (jsonOutput) {
            t.println(Json.encodeToString(matches))
            return
        }

        if (matches.isEmpty()) {
            t.println(gray("No salary benchmarks found for '$targetCompany'${if (city != null) " in $city" else ""}."))
            return
        }

        t.println(green("Found ${matches.size} match(es) for '$targetCompany':"))
        t.println(
            table {
                header {
                    row("Company", "City", "Categories", "Top Index")
                }
                body {
                    matches.forEach { m ->
                        val topIdx = m.categories.values.mapNotNull { it.index }.maxOrNull()?.toString() ?: "-"
                        val catCount = m.categories.size.toString()
                        row(m.company, m.city ?: "-", catCount, topIdx)
                    }
                }
            }
        )
    }
}
