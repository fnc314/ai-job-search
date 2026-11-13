package dev.fnc314.tools.jobtracker.enrichment

import dev.fnc314.tools.jobsearch.tracker.v1.CompanySector

/**
 * Resolves a company's sector from Hunter.io data, falling back to TECH default.
 *
 * This resolver maps Hunter.io industry/sector strings to the canonical
 * [CompanySector] enum values. It serves as the primary sector resolution
 * mechanism, with the hardcoded CompanySectorNormalizer as a secondary fallback.
 */
public object CompanySectorResolver {

    /**
     * Known industry/sector keyword mappings to canonical CompanySector values.
     * Hunter.io returns free-form strings; this map normalizes them.
     */
    private val sectorKeywords: Map<String, CompanySector> = mapOf(
        // FinTech / Financial Services
        "financial services" to CompanySector.FINTECH,
        "fintech" to CompanySector.FINTECH,
        "banking" to CompanySector.FINTECH,
        "insurance" to CompanySector.FINTECH,
        "payments" to CompanySector.FINTECH,
        "lending" to CompanySector.FINTECH,
        "wealth management" to CompanySector.FINTECH,
        "capital markets" to CompanySector.FINTECH,

        // Healthcare
        "healthcare" to CompanySector.HEALTHCARE,
        "health care" to CompanySector.HEALTHCARE,
        "medical" to CompanySector.HEALTHCARE,
        "pharmaceutical" to CompanySector.HEALTHCARE,
        "biotech" to CompanySector.HEALTHCARE,
        "biotechnology" to CompanySector.HEALTHCARE,
        "health technology" to CompanySector.HEALTHCARE,

        // Education
        "education" to CompanySector.EDUCATION,
        "edtech" to CompanySector.EDUCATION,
        "e-learning" to CompanySector.EDUCATION,

        // E-commerce / Retail
        "e-commerce" to CompanySector.ECOMMERCE,
        "ecommerce" to CompanySector.ECOMMERCE,
        "retail" to CompanySector.ECOMMERCE,
        "marketplace" to CompanySector.ECOMMERCE,

        // Media / Entertainment
        "media" to CompanySector.MEDIA,
        "entertainment" to CompanySector.MEDIA,
        "publishing" to CompanySector.MEDIA,
        "gaming" to CompanySector.MEDIA,

        // Manufacturing
        "manufacturing" to CompanySector.MANUFACTURING,
        "industrial" to CompanySector.MANUFACTURING,
        "automotive" to CompanySector.MANUFACTURING,

        // Energy
        "energy" to CompanySector.ENERGY,
        "oil & gas" to CompanySector.ENERGY,
        "renewable energy" to CompanySector.ENERGY,

        // Real Estate
        "real estate" to CompanySector.REALESTATE,
        "property management" to CompanySector.REALESTATE,

        // Transportation / Logistics
        "transportation" to CompanySector.TRANSPORTATION,
        "logistics" to CompanySector.TRANSPORTATION,
        "supply chain" to CompanySector.TRANSPORTATION,

        // Technology (default catch-all)
        "technology" to CompanySector.TECH,
        "software" to CompanySector.TECH,
        "it services" to CompanySector.TECH,
        "information technology" to CompanySector.TECH,
        "computer software" to CompanySector.TECH,
        "internet" to CompanySector.TECH,
        "saas" to CompanySector.TECH,
    )

    /**
     * Resolve a company's sector from Hunter.io data.
     *
     * Falls back to [CompanySector.TECH] if no matching keywords are found.
     *
     * @param info The Hunter.io company metadata, or `null` to return TECH default.
     * @return The resolved [CompanySector].
     */
    public fun resolveSector(info: HunterCompanyInfo?): CompanySector {
        if (info == null) return CompanySector.TECH

        // Try sector first, then industry, then subIndustry
        val candidates = listOfNotNull(info.sector, info.industry, info.subIndustry)

        for (candidate in candidates) {
            val lower = candidate.lowercase()
            for ((keyword, sector) in sectorKeywords) {
                if (lower.contains(keyword)) {
                    return sector
                }
            }
        }

        return CompanySector.TECH
    }

    /**
     * Resolve a company's sector from raw industry/sector strings.
     *
     * Useful when Hunter.io data is not available but some industry string is known.
     *
     * @param industryOrSector The raw industry or sector string.
     * @return The resolved [CompanySector], defaulting to TECH.
     */
    public fun resolveSectorFromText(industryOrSector: String?): CompanySector {
        if (industryOrSector.isNullOrBlank()) return CompanySector.TECH

        val lower = industryOrSector.lowercase()
        for ((keyword, sector) in sectorKeywords) {
            if (lower.contains(keyword)) {
                return sector
            }
        }

        return CompanySector.TECH
    }
}
