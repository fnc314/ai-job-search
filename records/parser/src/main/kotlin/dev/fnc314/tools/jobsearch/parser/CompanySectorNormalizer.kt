package dev.fnc314.tools.jobsearch.parser

import dev.fnc314.tools.jobsearch.tracker.v1.CompanySector
import java.util.Locale

/**
 * Normalizes industry strings, keywords, and tokens to canonical [CompanySector] enumeration values.
 *
 * NOTE: Hunter.io Company Enrichment API is now the primary source for company metadata
 * (sector, LinkedIn URL, logo, etc.). This normalizer serves as a secondary fallback
 * for companies where Hunter.io returns no data.
 */
public object CompanySectorNormalizer {

    /** Hardcoded sector mappings — fallback for companies where Hunter.io returns no data. */
    private val KNOWN_COMPANY_SECTORS: Map<String, CompanySector> = mapOf(
        // FinTech / Banking / Crypto
        "affirm" to CompanySector.FINTECH,
        "bankjoy" to CompanySector.FINTECH,
        "capital-one" to CompanySector.FINTECH,
        "coinbase" to CompanySector.FINTECH,
        "edward-jones" to CompanySector.FINTECH,
        "fidelity-investments" to CompanySector.FINTECH,
        "huntington" to CompanySector.FINTECH,
        "jpmorgan-chase" to CompanySector.FINTECH,
        "pnc" to CompanySector.FINTECH,
        "polymarket" to CompanySector.FINTECH,
        "ramp" to CompanySector.FINTECH,
        "robinhood" to CompanySector.FINTECH,
        "sentilink" to CompanySector.FINTECH,
        "spoton" to CompanySector.FINTECH,
        "unit21" to CompanySector.FINTECH,
        "vanguard" to CompanySector.FINTECH,

        // Healthcare / HealthTech / Medical
        "baxter-international" to CompanySector.HEALTHCARE,
        "boston-scientific" to CompanySector.HEALTHCARE,
        "butterfly-network" to CompanySector.HEALTHCARE,
        "centene" to CompanySector.HEALTHCARE,
        "grow-therapy" to CompanySector.HEALTHCARE,
        "hims-hers" to CompanySector.HEALTHCARE,
        "honor" to CompanySector.HEALTHCARE,
        "karoo-health" to CompanySector.HEALTHCARE,
        "osmind" to CompanySector.HEALTHCARE,

        // E-Commerce / Retail
        "american-eagle-outfitters" to CompanySector.RETAIL,
        "ebay" to CompanySector.E_COMMERCE,
        "fanatics" to CompanySector.E_COMMERCE,
        "grove-collaborative" to CompanySector.E_COMMERCE,
        "iherb" to CompanySector.E_COMMERCE,
        "instacart" to CompanySector.E_COMMERCE,
        "sharkninja" to CompanySector.E_COMMERCE,
        "slickdeals" to CompanySector.E_COMMERCE,
        "ulta-beauty" to CompanySector.RETAIL,

        // Automotive / Transportation
        "car-next-door" to CompanySector.AUTOMOTIVE,
        "doordash" to CompanySector.TECH,
        "ford" to CompanySector.AUTOMOTIVE,
        "general-motors" to CompanySector.AUTOMOTIVE,
        "lime" to CompanySector.AUTOMOTIVE,
        "scout-motors" to CompanySector.AUTOMOTIVE,
        "uber" to CompanySector.TECH,
        "waymo" to CompanySector.AUTOMOTIVE,

        // Media / Entertainment
        "disney" to CompanySector.MEDIA,
        "major-league-baseball" to CompanySector.MEDIA,
        "medium" to CompanySector.MEDIA,
        "npr" to CompanySector.MEDIA,
        "paramount" to CompanySector.MEDIA,
        "playlist" to CompanySector.MEDIA,
        "reddit" to CompanySector.MEDIA,
        "roku" to CompanySector.MEDIA,
        "washington-post" to CompanySector.MEDIA,

        // Education
        "carnegie-mellon-university" to CompanySector.EDUCATION,

        // Consulting / Professional Services
        "deloitte" to CompanySector.CONSULTING,
        "thomson-reuters" to CompanySector.CONSULTING,

        // Technology / Software
        "anthropic" to CompanySector.TECH,
        "bitwarden" to CompanySector.TECH,
        "bigcommerce" to CompanySector.TECH,
        "block" to CompanySector.FINTECH,
        "canary-technologies" to CompanySector.TECH,
        "classpass" to CompanySector.TECH,
        "crestron-electronics" to CompanySector.TECH,
        "dropbox" to CompanySector.TECH,
        "duolingo" to CompanySector.TECH,
        "eight-sleep" to CompanySector.TECH,
        "fetch" to CompanySector.TECH,
        "future" to CompanySector.TECH,
        "github" to CompanySector.TECH,
        "google" to CompanySector.TECH,
        "hireeazy" to CompanySector.TECH,
        "meta" to CompanySector.TECH,
        "microsoft" to CompanySector.TECH,
        "netflix" to CompanySector.TECH,
        "nextdoor" to CompanySector.TECH,
        "onx" to CompanySector.TECH,
        "openai" to CompanySector.TECH,
        "radar" to CompanySector.TECH,
        "real" to CompanySector.TECH,
        "revenuecat" to CompanySector.TECH,
        "salesforce" to CompanySector.TECH,
        "seatgeek" to CompanySector.TECH,
        "snap" to CompanySector.TECH,
        "speechify" to CompanySector.TECH,
        "talentry" to CompanySector.TECH,
        "teamsnap" to CompanySector.TECH,
        "textnow" to CompanySector.TECH,
        "toast" to CompanySector.TECH,
        "touchlab" to CompanySector.TECH,
        "trade-desk" to CompanySector.TECH,
        "twitter2" to CompanySector.TECH,
        "unity" to CompanySector.TECH,
        "yinzcam" to CompanySector.TECH,
        "zillow" to CompanySector.TECH,
        "ziprecruiter" to CompanySector.TECH
    )

    private val INDUSTRY_KEYWORD_RULES: List<Pair<List<String>, CompanySector>> = listOf(
        listOf("financ", "bank", "invest", "credit") to CompanySector.FINTECH,
        listOf("health", "medical", "hospital", "biotech", "pharma") to CompanySector.HEALTHCARE,
        listOf("e-commerce", "ecommerce", "apparel", "retail") to CompanySector.E_COMMERCE,
        listOf("auto", "motor", "vehicle", "transport") to CompanySector.AUTOMOTIVE,
        listOf("media", "entertainment", "broadcast", "publishing") to CompanySector.MEDIA,
        listOf("education", "university", "school") to CompanySector.EDUCATION,
        listOf("consulting", "professional services", "legal") to CompanySector.CONSULTING,
        listOf("aerospace", "defense", "aviation") to CompanySector.AEROSPACE,
        listOf("energy", "oil", "utility") to CompanySector.ENERGY,
        listOf("telecom", "wireless") to CompanySector.TELECOM,
        listOf("software", "tech", "internet", "data") to CompanySector.TECH
    )

    /**
     * Resolves the canonical [CompanySector] for a company by ID, industry string, or name tokens.
     */
    @JvmStatic
    public fun normalize(
        companyId: String? = null,
        industryOrSector: String? = null,
        companyName: String? = null
    ): CompanySector {
        if (!companyId.isNullOrBlank()) {
            val normalizedId = DocumentIdGenerator.slug(companyId).ifEmpty { companyId.trim().lowercase(Locale.ROOT) }
            KNOWN_COMPANY_SECTORS[normalizedId]?.let { return it }
            KNOWN_COMPANY_SECTORS[companyId.trim().lowercase(Locale.ROOT)]?.let { return it }
        }

        if (!industryOrSector.isNullOrBlank()) {
            val fromIndustry = parseFromIndustryString(industryOrSector)
            if (fromIndustry != CompanySector.COMPANY_SECTOR_UNSPECIFIED) {
                return fromIndustry
            }
        }

        if (!companyName.isNullOrBlank()) {
            val fromName = parseFromTokens(companyName)
            if (fromName != CompanySector.COMPANY_SECTOR_UNSPECIFIED) {
                return fromName
            }
        }

        return CompanySector.TECH
    }

    /**
     * Resolves the LinkedIn company profile URL.
     * Returns null — Hunter.io is now the primary source for LinkedIn handles.
     * Callers should use the LinkedIn handle from Hunter.io enrichment instead.
     */
    @JvmStatic
    public fun resolveLinkedinUrl(companyId: String): String? {
        // Hunter.io provides LinkedIn handles directly — no longer using hardcoded slugs.
        return null
    }

    /**
     * Formats a [CompanySector] enum into a human-readable display string.
     */
    @JvmStatic
    public fun formatDisplay(sector: CompanySector): String {
        return when (sector) {
            CompanySector.TECH -> "Technology"
            CompanySector.FINTECH -> "FinTech"
            CompanySector.HEALTHCARE -> "Healthcare"
            CompanySector.E_COMMERCE -> "E-Commerce"
            CompanySector.AUTOMOTIVE -> "Automotive"
            CompanySector.MEDIA -> "Media"
            CompanySector.EDUCATION -> "Education"
            CompanySector.CONSULTING -> "Consulting"
            CompanySector.AEROSPACE -> "Aerospace"
            CompanySector.RETAIL -> "Retail"
            CompanySector.ENERGY -> "Energy"
            CompanySector.TELECOM -> "Telecom"
            CompanySector.COMPANY_SECTOR_UNSPECIFIED,
            CompanySector.UNRECOGNIZED -> "Technology"
        }
    }

    private fun parseFromIndustryString(industry: String): CompanySector {
        val lower = industry.lowercase(Locale.ROOT)
        for ((keywords, sector) in INDUSTRY_KEYWORD_RULES) {
            if (keywords.any { lower.contains(it) }) {
                return sector
            }
        }
        return CompanySector.COMPANY_SECTOR_UNSPECIFIED
    }

    private fun parseFromTokens(text: String): CompanySector {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("bank") || lower.contains("pay") || lower.contains("capital") -> CompanySector.FINTECH
            lower.contains("health") || lower.contains("care") || lower.contains("med") -> CompanySector.HEALTHCARE
            lower.contains("motor") || lower.contains("auto") -> CompanySector.AUTOMOTIVE
            lower.contains("news") || lower.contains("post") || lower.contains("radio") -> CompanySector.MEDIA
            else -> CompanySector.COMPANY_SECTOR_UNSPECIFIED
        }
    }
}
