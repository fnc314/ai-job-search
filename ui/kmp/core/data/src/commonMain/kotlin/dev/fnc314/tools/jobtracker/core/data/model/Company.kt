package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.time.Instant

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
 */
public data class Company(
    public val id: String,
    public val name: String,
    public val aliases: List<String>,
    public val website: String?,
    public val industry: String?,
    public val createdAt: Instant?,
    public val updatedAt: Instant?,
)
