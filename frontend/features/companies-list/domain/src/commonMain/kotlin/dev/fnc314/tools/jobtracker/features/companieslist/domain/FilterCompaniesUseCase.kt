package dev.fnc314.tools.jobtracker.features.companieslist.domain

import dev.fnc314.tools.jobtracker.core.data.model.Company

/**
 * Filter criteria for segmenting companies by application stage and status.
 */
public enum class CompanyStatusFilter {
    ALL,
    OPEN_APPLICATIONS,
    ONGOING_INTERVIEW,
    OPEN_OFFER;

    public val isFiltered: Boolean get() = this != ALL
}

public class FilterCompaniesUseCase {
    public operator fun invoke(
        companies: List<Company>,
        query: String = "",
        statusFilter: CompanyStatusFilter = CompanyStatusFilter.ALL,
    ): List<Company> {
        val trimmed = query.trim().lowercase()

        return companies.filter { company ->
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                company.name.lowercase().contains(trimmed) ||
                    company.industry?.lowercase()?.contains(trimmed) == true ||
                    company.aliases.any { it.lowercase().contains(trimmed) } ||
                    company.metrics?.locations?.any { it.lowercase().contains(trimmed) } == true
            }

            if (!matchesQuery) return@filter false

            val counts = company.metrics?.counts
            when (statusFilter) {
                CompanyStatusFilter.ALL -> true
                CompanyStatusFilter.OPEN_APPLICATIONS -> {
                    (counts?.submitted ?: 0) > 0 || (counts?.interview ?: 0) > 0
                }
                CompanyStatusFilter.ONGOING_INTERVIEW -> {
                    (counts?.interview ?: 0) > 0
                }
                CompanyStatusFilter.OPEN_OFFER -> {
                    (counts?.offer ?: 0) > 0
                }
            }
        }
    }
}
