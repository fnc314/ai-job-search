package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.time.Instant

/**
 * Image encoding format for company logos.
 */
public enum class LogoFormat {
    UNKNOWN,
    AVIF,
    WEBP,
    PNG,
    SVG,
}

/**
 * 2D image aspect shape classification.
 */
public enum class LogoShape {
    UNKNOWN,
    SQUARE,
    RECTANGLE,
}

/**
 * Domain entity representing an employer or hiring organization.
 *
 * @property id Canonical company slug identifier (e.g., `"pnc"`).
 * @property name Official company name (e.g., `"PNC Financial Services"`).
 * @property aliases Known company trading names, brand variants, or subsidiary labels.
 * @property website Official corporate or career website URL.
 * @property industry Target market sector or business category (e.g., `"FinTech"`).
 * @property createdAt Timestamp when the company record was initially registered.
 * @property updatedAt Timestamp when the company profile was last updated.
 * @property logoUrl Direct public URL to the company logo on Hunter.io or CDN.
 * @property logoFormat Detected image format (AVIF, WEBP, PNG, SVG).
 * @property logoWidth Detected image width in pixels.
 * @property logoHeight Detected image height in pixels.
 * @property logoShape Aspect shape (SQUARE vs RECTANGLE).
 * @property logoFetchedAt Timestamp when the logo was fetched and verified.
 * @property linkedinUrl LinkedIn company page URL.
 * @property hunterIoId Stable Hunter.io company identifier (UUID).
 * @property description Company description from Hunter.io.
 * @property foundedYear Year the company was founded.
 * @property companyType Company type (e.g., "public company", "privately held").
 * @property employees Employee range string (e.g., "10K-50K").
 * @property employeesCount Exact employee count.
 * @property location Full address string from Hunter.io.
 * @property city City from Hunter.io geo data.
 * @property state State from Hunter.io geo data.
 * @property country Country from Hunter.io geo data.
 * @property subIndustry Sub-industry from Hunter.io category data.
 * @property twitterUrl Twitter/X company profile URL.
 * @property crunchbaseUrl Crunchbase company profile URL.
 * @property lastHunterEnrichedAt Timestamp of the last successful Hunter.io enrichment.
 */
public data class Company(
    public val id: String,
    public val name: String,
    public val aliases: List<String>,
    public val website: String?,
    public val industry: String?,
    public val createdAt: Instant?,
    public val updatedAt: Instant?,
    public val logoUrl: String? = null,
    public val logoFormat: LogoFormat = LogoFormat.UNKNOWN,
    public val logoWidth: Int = 0,
    public val logoHeight: Int = 0,
    public val logoShape: LogoShape = LogoShape.UNKNOWN,
    public val logoFetchedAt: Instant = Instant.DISTANT_PAST,
    public val metrics: CompanyMetrics? = null,
    public val linkedinUrl: String? = null,
    public val hunterIoId: String? = null,
    public val description: String? = null,
    public val foundedYear: Int? = null,
    public val companyType: String? = null,
    public val employees: String? = null,
    public val employeesCount: Int? = null,
    public val location: String? = null,
    public val city: String? = null,
    public val state: String? = null,
    public val country: String? = null,
    public val subIndustry: String? = null,
    public val twitterUrl: String? = null,
    public val crunchbaseUrl: String? = null,
    public val lastHunterEnrichedAt: Instant? = null,
) {
    /** Whether the logo has equal width and height dimensions. */
    public val isSquare: Boolean get() = logoShape == LogoShape.SQUARE
}

