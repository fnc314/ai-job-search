package dev.fnc314.aijobtracker.records.parser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.Normalizer

/**
 * Model representing metadata in a salary benchmarks file.
 */
@Serializable
public data class SalaryMetadata(
    public val source: String? = null,
    public val indexBaseline: Double? = null,
    public val indexLabel: String? = "Index",
    public val baselineDescription: String? = null,
)

/**
 * Benchmark statistics for a specific job category within a company.
 */
@Serializable
public data class SalaryCategoryStats(
    public val count: Int? = null,
    public val index: Double? = null,
    public val median: Double? = null,
    public val mean: Double? = null,
)

/**
 * Company salary benchmark entry.
 */
@Serializable
public data class CompanySalaryBenchmark(
    public val company: String,
    public val city: String? = null,
    public val categories: Map<String, SalaryCategoryStats> = emptyMap(),
)

/**
 * Complete salary benchmark dataset model.
 */
@Serializable
public data class SalaryDataset(
    public val metadata: SalaryMetadata? = null,
    public val companies: List<CompanySalaryBenchmark> = emptyList(),
)

/**
 * Domain engine for salary data parsing, validation, and fuzzy search matching.
 */
public object SalaryBenchmarkEngine {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val STRIP_PATTERNS = listOf(
        Regex("""\ba/s\b""", RegexOption.IGNORE_CASE),
        Regex("""\baps\b""", RegexOption.IGNORE_CASE),
        Regex("""\bi/s\b""", RegexOption.IGNORE_CASE),
        Regex("""\bp/s\b""", RegexOption.IGNORE_CASE),
        Regex("""\bk/s\b""", RegexOption.IGNORE_CASE),
        Regex("""\bivs\b""", RegexOption.IGNORE_CASE),
        Regex("""\bamba\b""", RegexOption.IGNORE_CASE),
        Regex("""\ba\.m\.b\.a\.\b""", RegexOption.IGNORE_CASE),
        Regex("""\(vg\)""", RegexOption.IGNORE_CASE),
        Regex("""\(.*?\)"""),
        Regex("""\bdanmark\b""", RegexOption.IGNORE_CASE),
        Regex("""\bdenmark\b""", RegexOption.IGNORE_CASE),
        Regex("""\bscandinavia\b""", RegexOption.IGNORE_CASE),
        Regex("""\bnordic\b""", RegexOption.IGNORE_CASE),
        Regex("""\bgroup\b""", RegexOption.IGNORE_CASE),
        Regex("""\bholding\b""", RegexOption.IGNORE_CASE),
        Regex(""",\s*.*$"""),
    )

    private val SPELLING_VARIANTS = mapOf(
        'ø' to "o", 'æ' to "ae", 'å' to "aa",
        'ö' to "o", 'ä' to "ae", 'ü' to "u"
    )

    /**
     * Normalizes a company name for fuzzy comparison.
     */
    public fun normalizeCompanyName(name: String): String {
        var s = name.trim().lowercase()
        for (pattern in STRIP_PATTERNS) {
            s = pattern.replace(s, "")
        }
        val sb = StringBuilder()
        for (ch in s) {
            sb.append(SPELLING_VARIANTS[ch] ?: ch)
        }
        s = sb.toString()
        // Normalize unicode accents
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("""\p{M}"""), "")
        return s.replace(Regex("""[^a-z0-9]"""), " ").replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Loads and parses a salary dataset from the specified file.
     */
    public fun loadDataset(file: File): Result<SalaryDataset> {
        if (!file.exists()) {
            return Result.failure(IllegalArgumentException("Salary data file not found: ${file.absolutePath}"))
        }
        return try {
            val content = file.readText()
            Result.success(json.decodeFromString<SalaryDataset>(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Searches the dataset for matching companies.
     */
    public fun search(dataset: SalaryDataset, query: String, city: String? = null): List<CompanySalaryBenchmark> {
        val normQuery = normalizeCompanyName(query)
        val normCity = city?.trim()?.lowercase()

        return dataset.companies.filter { entry ->
            val normCompany = normalizeCompanyName(entry.company)
            val matchesCompany = normCompany.contains(normQuery) || normQuery.contains(normCompany)
            val matchesCity = normCity == null || (entry.city?.lowercase()?.contains(normCity) == true)
            matchesCompany && matchesCity
        }
    }
}
