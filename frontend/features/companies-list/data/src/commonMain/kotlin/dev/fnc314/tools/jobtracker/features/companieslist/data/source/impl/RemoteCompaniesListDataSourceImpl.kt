package dev.fnc314.tools.jobtracker.features.companieslist.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.CompanyMetrics
import dev.fnc314.tools.jobtracker.core.data.model.LogoFormat
import dev.fnc314.tools.jobtracker.core.data.model.LogoShape
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.core.data.source.RemoteRecord
import dev.fnc314.tools.jobtracker.core.data.util.InstantParser
import dev.fnc314.tools.jobtracker.features.companieslist.data.source.RemoteCompaniesListDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val STOP_WORDS = setOf("the", "a", "an", "of", "and")
private val CORP_SUFFIXES = setOf("inc", "llc", "corp", "corporation", "ltd", "co", "company")
private val SLUG_STRIP_REGEX = Regex("[^a-z0-9]+")
private val PARENTHETICAL_REGEX = Regex("(?<=\\w)\\s*\\([A-Za-z0-9]+\\)")

private val COMPANY_SLUG_ALIASES: Map<String, String> = mapOf(
    "meta-platforms" to "meta",
    "facebook" to "meta",
    "ford-motor-company" to "ford-motor",
    "ford" to "ford-motor",
    "alphabet" to "google",
    "square" to "block",
    "pnc-financial-services" to "pnc",
    "pnc-bank" to "pnc",
    "jpmorgan" to "jpmorgan-chase",
    "jp-morgan" to "jpmorgan-chase",
    "jp-morgan-chase" to "jpmorgan-chase",
)

internal fun canonicalCompanySlug(raw: String): String {
    if (raw.isBlank()) return ""
    val stripped = raw.trim().replace(PARENTHETICAL_REGEX, "")
    val rawSlug = SLUG_STRIP_REGEX.replace(stripped.lowercase(), "-").trim('-')
    if (rawSlug.isEmpty()) return ""
    val tokens = rawSlug.split("-").filter { it.isNotEmpty() }
    val filtered = tokens.filter { it !in STOP_WORDS && it !in CORP_SUFFIXES }
    val finalTokens = filtered.ifEmpty { tokens }
    val deduped = mutableListOf<String>()
    for (token in finalTokens) {
        if (deduped.lastOrNull() != token) {
            deduped.add(token)
        }
    }
    val candidate = deduped.joinToString("-")
    return COMPANY_SLUG_ALIASES[candidate] ?: candidate
}

internal class RemoteCompaniesListDataSourceImpl(
    private val remoteDataSource: RemoteDataSource,
) : RemoteCompaniesListDataSource {

    override suspend operator fun invoke(): List<Company> {
        return withContext(Dispatchers.Default) {
            val mapped = remoteDataSource
                .getCollection("companies")
                .map(::mapRecordToCompany)
            deduplicateCompanies(mapped)
        }
    }

    private fun deduplicateCompanies(companies: List<Company>): List<Company> {
        return companies
            .groupBy { company ->
                canonicalCompanySlug(company.name)
                    .ifEmpty { canonicalCompanySlug(company.id) }
                    .ifEmpty { company.id }
            }
            .map { (canonicalKey, group) ->
                if (group.size == 1) {
                    group.first()
                } else {
                    mergeCompanyGroup(canonicalKey, group)
                }
            }
    }

    private fun mergeCompanyGroup(canonicalKey: String, group: List<Company>): Company {
        // Prefer the record whose ID matches the canonical slug or has higher total applications
        val primary = group.maxWithOrNull(
            compareBy<Company> { it.id == canonicalKey }
                .thenBy { it.metrics?.counts?.total ?: 0 }
                .thenBy { !it.logoUrl.isNullOrBlank() }
                .thenBy { !it.website.isNullOrBlank() },
        ) ?: group.first()

        val mergedAliases = linkedSetOf<String>()
        for (item in group) {
            mergedAliases.addAll(item.aliases)
            if (item.name != primary.name) {
                mergedAliases.add(item.name)
            }
            if (item.id != primary.id) {
                mergedAliases.add(item.id)
            }
        }

        val bestMetrics = group
            .mapNotNull { it.metrics }
            .maxByOrNull { it.counts.total }
            ?: primary.metrics

        val bestLogoItem = group.firstOrNull { !it.logoUrl.isNullOrBlank() } ?: primary

        return primary.copy(
            aliases = mergedAliases.toList(),
            website = primary.website ?: group.firstNotNullOfOrNull { it.website },
            industry = primary.industry ?: group.firstNotNullOfOrNull { it.industry },
            logoUrl = bestLogoItem.logoUrl,
            logoFormat = bestLogoItem.logoFormat,
            logoWidth = bestLogoItem.logoWidth,
            logoHeight = bestLogoItem.logoHeight,
            logoShape = bestLogoItem.logoShape,
            logoFetchedAt = bestLogoItem.logoFetchedAt,
            metrics = bestMetrics,
            linkedinUrl = primary.linkedinUrl ?: group.firstNotNullOfOrNull { it.linkedinUrl },
        )
    }

    private fun mapRecordToCompany(record: RemoteRecord): Company {
        val aliases = (record.fields["aliases"] as? List<*>)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val logoFormatStr = (record.getString("logoFormat") ?: record.getString("logo_format")).orEmpty().uppercase()
        val logoShapeStr = (record.getString("logoShape") ?: record.getString("logo_shape")).orEmpty().uppercase()

        val logoFormat = runCatching { LogoFormat.valueOf(logoFormatStr) }.getOrDefault(LogoFormat.UNKNOWN)
        val logoShape = runCatching { LogoShape.valueOf(logoShapeStr) }.getOrDefault(LogoShape.UNKNOWN)

        val metricsMap = record.fields["metrics"] as? Map<*, *>
        val metrics = CompanyMetrics.fromMap(metricsMap)

        return Factory.createCompany(
            id = record.id,
            name = record.nonBlankString("name")
                ?: record.nonBlankString("company")
                ?: record.id,
            aliases = aliases,
            website = record.getString("website"),
            industry = record.getString("sector") ?: record.getString("industry"),
            createdAt = InstantParser.parse(
                record.fields["createdAt"] ?: record.fields["created_at"],
            ),
            updatedAt = InstantParser.parse(
                record.fields["updatedAt"] ?: record.fields["updated_at"],
            ),
            logoUrl = record.getString("logoUrl"),
            logoFormat = logoFormat,
            logoWidth = (record.getLong("logoWidth") ?: record.getLong("logo_width"))?.toInt() ?: 0,
            logoHeight = (record.getLong("logoHeight") ?: record.getLong("logo_height"))?.toInt() ?: 0,
            logoShape = logoShape,
            logoFetchedAt = InstantParser.parse(record.fields["logoFetchedAt"]),
            metrics = metrics,
            linkedinUrl = record.getString("linkedinUrl"),
        )
    }

    private fun RemoteRecord.nonBlankString(key: String): String? =
        getString(key)?.trim()?.takeIf { it.isNotEmpty() }
}
