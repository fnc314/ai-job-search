package dev.fnc314.tools.jobsearch.parser

import java.text.Normalizer

/**
 * Derives stable, filesystem-safe identifiers from free-form company and role names.
 *
 * This object is the **single source of truth** across the monorepo (`records`, `backend`,
 * and `frontend`) for deriving canonical `companyId` slugs and `applications` document IDs.
 * Never use ad-hoc regular expressions to derive `companyId` values.
 *
 * Mirrors the Python `contract.normalize_dir_name` / `contract.slug` / `contract.documentId`
 * behaviour so that document IDs, GCS paths, and Sheet headers remain byte-identical
 * after the Kotlin-only migration.
 *
 * Rules (applied in order):
 *   1. Strip parentheticals following a word (e.g. " (MLB)").
 *   2. Normalize Unicode (NFKD) and drop non-ASCII code points.
 *   3. Expand title contractions ("sr." → "senior", etc.).
 *   4. Strip stop words and corporate suffixes.
 *   5. Convert to lower-kebab-case.
 *   6. Deduplicate adjacent identical tokens.
 *   7. Apply canonical company alias resolution (e.g. `"meta-platforms"` → `"meta"`).
 */
public object DocumentIdGenerator {

    private const val ASCII_UPPER_BOUND = 128
    private val slugStrip = Regex("[^a-z0-9]+")

    private val dirContractions = mapOf(
        "sr" to "senior",
        "sr." to "senior",
        "jr" to "junior",
        "jr." to "junior",
        "asst" to "assistant",
        "asst." to "assistant",
        "assoc" to "associate",
        "assoc." to "associate",
    )

    private val stopWords = setOf("the", "a", "an", "of", "and")
    private val corpSuffixes = setOf("inc", "llc", "corp", "corporation", "ltd", "co", "company")

    /**
     * Canonical company slug aliases mapping known employer name variants to a single `companyId`.
     */
    private val companySlugAliases: Map<String, String> = mapOf(
        "meta-platforms" to "meta",
        "facebook" to "meta",
        "ford-motor-company" to "ford",
        "ford-motor" to "ford",
        "alphabet" to "google",
        "square" to "block",
        "pnc-financial-services" to "pnc",
        "pnc-bank" to "pnc",
        "jpmorgan" to "jpmorgan-chase",
        "jp-morgan" to "jpmorgan-chase",
        "jp-morgan-chase" to "jpmorgan-chase",
        "twitter2" to "twitter",
        "capitalone" to "capital-one",
        "edwardjones" to "edward-jones",
        "eightsleep" to "eight-sleep",
        "jpmorganchase" to "jpmorgan-chase",
        "homedepot" to "home-depot",
        "thompsonreuters" to "thomson-reuters",
        "colossustechnologies" to "colossus-technologies",
        "fitnesstech" to "fitness-tech",
        "planetfitness" to "planet-fitness",
        "paradigmnatl" to "paradigm-natl",
        "classlist" to "classlist",
        "class-list" to "classlist",
        "open-ai" to "openai",
        "yinz-cam" to "yinzcam",
        "on-x" to "onx",
        "spot-on" to "spoton",
    )

    /**
     * Normalize a company or role name for use in lower-kebab-case directory paths and document IDs.
     *
     * Example transformations:
     *   "The Washington Post"             ->  "washington-post"
     *   "Scout Motors Inc."               ->  "scout-motors"
     *   "Sr. Software Engineer, Android"  ->  "senior-software-engineer-android"
     *   "Major League Baseball (MLB)"     ->  "major-league-baseball"
     *   "Snap Inc."                       ->  "snap"
     *   "Ford Motor Company"              ->  "ford"
     *   "Meta Platforms, Inc."            ->  "meta"
     *   "EBay"                            ->  "ebay"
     *   "onX"                             ->  "onx"
     *
     * @param raw The raw company or role name.
     * @return A lower-kebab-case slug, or an empty string if nothing remains.
     */
    public fun normalizeDirName(raw: String): String {
        if (raw.isBlank()) return ""

        // 1. Strip parentheticals like " (MLB)"
        var text = raw.trim().replace(Regex("(?<=\\w)\\s*\\([A-Za-z0-9]+\\)"), "")

        // 2. Unicode NFKD normalization, drop non-ASCII
        text = Normalizer.normalize(text, Normalizer.Form.NFKD)
            .filter { c -> c.code < ASCII_UPPER_BOUND }

        val rawSlug = slugStrip.replace(text.lowercase(), "-").trim('-')
        if (rawSlug.isEmpty()) return ""

        val tokens = rawSlug.split("-")

        // 3. Expand contractions
        val expanded = tokens.mapNotNull { token ->
            if (token.isEmpty()) null
            else dirContractions[token] ?: token
        }

        // 4. Strip stop words and corporate suffixes
        val filtered = expanded.filter { w -> w !in stopWords && w !in corpSuffixes }
        val finalTokens = if (filtered.isNotEmpty()) filtered else expanded

        // 5. Deduplicate adjacent identical tokens
        val deduped = mutableListOf<String>()
        for (word in finalTokens) {
            if (deduped.lastOrNull() != word) {
                deduped.add(word)
            }
        }

        val candidate = deduped.joinToString("-")
        return companySlugAliases[candidate] ?: candidate
    }

    /**
     * ASCII kebab slug for `companyId` and document IDs.
     *
     * Delegates to [normalizeDirName] and falls back to [fallback] when the result is empty.
     *
     * @param value The raw company or role name.
     * @param fallback Returned when the slug is empty.
     * @return A canonical lower-kebab-case slug.
     */
    public fun slug(value: String, fallback: String = ""): String {
        if (value.isBlank()) return fallback
        return normalizeDirName(value).ifEmpty { fallback }
    }

    /**
     * Stable application document ID: `slug(company) + "--" + slug(role)`, or bare company slug.
     *
     * Byte-identical to the scheme already present in the CSV archive, which is what lets
     * reconciliation assert an exact match on all historical rows.
     *
     * @param company The company name (required).
     * @param role The optional role title.
     * @return A document ID string.
     * @throws ContractError if the company slug cannot be derived.
     */
    public fun documentId(company: String, role: String?): String {
        val companySlug = slug(company)
        if (companySlug.isEmpty()) {
            throw ContractError("cannot derive a document ID without a company name")
        }
        val roleSlug = role?.let { slug(it) }.orEmpty()
        return if (roleSlug.isNotEmpty()) "$companySlug--$roleSlug" else companySlug
    }
}

/**
 * Thrown when a document violates the tracker contract.
 *
 * @param message Description of the contract violation.
 */
public class ContractError(message: String) : IllegalArgumentException(message)
