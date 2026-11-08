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
) {
    /** Whether the logo has equal width and height dimensions. */
    public val isSquare: Boolean get() = logoShape == LogoShape.SQUARE
}

