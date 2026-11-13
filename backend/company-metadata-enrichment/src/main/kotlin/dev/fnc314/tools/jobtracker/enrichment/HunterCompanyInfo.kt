package dev.fnc314.tools.jobtracker.enrichment

import kotlinx.serialization.Serializable

/**
 * Company metadata returned by the Hunter.io Company Enrichment API.
 */
@Serializable
public data class HunterCompanyInfo(
    val id: String?,
    val name: String?,
    val industry: String?,
    val subIndustry: String?,
    val sector: String?,
    val linkedinHandle: String?,
    val twitterHandle: String?,
    val crunchbaseHandle: String?,
    val logoUrl: String?,
    val description: String?,
    val foundedYear: Int?,
    val companyType: String?,
    val employees: String?,
    val employeesCount: Int?,
    val location: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val tags: List<String>?,
    val raised: Long?
)
