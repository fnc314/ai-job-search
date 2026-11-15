package dev.fnc314.tools.jobtracker.enrichment

import kotlinx.serialization.Serializable

/**
 * Company metadata returned by the Hunter.io Company Enrichment API.
 *
 * @property id Hunter entity identifier.
 * @property name Official legal or trade company name.
 * @property industry Primary industry taxonomy name.
 * @property subIndustry Specific sub-industry classification.
 * @property sector Macro sector grouping.
 * @property linkedinHandle Company LinkedIn profile handle or URL.
 * @property twitterHandle Company Twitter / X handle.
 * @property crunchbaseHandle Company Crunchbase profile handle.
 * @property logoUrl Direct URL to company logo asset.
 * @property description Public company overview summary.
 * @property foundedYear Calendar year company was founded.
 * @property companyType Entity type (e.g. PRIVATELY_HELD, PUBLIC).
 * @property employees Descriptive employee headcount range string.
 * @property employeesCount Estimated employee count integer.
 * @property location Formatted headquarters location string.
 * @property city Headquarters city.
 * @property state Headquarters state or province.
 * @property country Headquarters country code or name.
 * @property tags Keyword tags associated with company offerings.
 * @property raised Total venture or funding capital raised in USD.
 */
@Serializable
public data class HunterCompanyInfo(
    /** Hunter entity identifier. */
    public val id: String?,
    /** Official legal or trade company name. */
    public val name: String?,
    /** Primary industry taxonomy name. */
    public val industry: String?,
    /** Specific sub-industry classification. */
    public val subIndustry: String?,
    /** Macro sector grouping. */
    public val sector: String?,
    /** Company LinkedIn profile handle or URL. */
    public val linkedinHandle: String?,
    /** Company Twitter / X handle. */
    public val twitterHandle: String?,
    /** Company Crunchbase profile handle. */
    public val crunchbaseHandle: String?,
    /** Direct URL to company logo asset. */
    public val logoUrl: String?,
    /** Public company overview summary. */
    public val description: String?,
    /** Calendar year company was founded. */
    public val foundedYear: Int?,
    /** Entity type (e.g. PRIVATELY_HELD, PUBLIC). */
    public val companyType: String?,
    /** Descriptive employee headcount range string. */
    public val employees: String?,
    /** Estimated employee count integer. */
    public val employeesCount: Int?,
    /** Formatted headquarters location string. */
    public val location: String?,
    /** Headquarters city. */
    public val city: String?,
    /** Headquarters state or province. */
    public val state: String?,
    /** Headquarters country code or name. */
    public val country: String?,
    /** Keyword tags associated with company offerings. */
    public val tags: List<String>?,
    /** Total venture or funding capital raised in USD. */
    public val raised: Long?
)
