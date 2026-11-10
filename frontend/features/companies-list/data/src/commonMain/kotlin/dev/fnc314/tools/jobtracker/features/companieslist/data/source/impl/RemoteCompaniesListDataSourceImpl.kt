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

internal class RemoteCompaniesListDataSourceImpl(
    private val remoteDataSource: RemoteDataSource,
) : RemoteCompaniesListDataSource {

    override suspend operator fun invoke(): List<Company> {
        return withContext(Dispatchers.Default) {
            remoteDataSource
                .getCollection("companies")
                .map(::mapRecordToCompany)
        }
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
