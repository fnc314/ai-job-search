package dev.fnc314.tools.jobtracker.features.companieslist.domain

import dev.fnc314.tools.jobtracker.core.data.model.Company

public class FilterCompaniesUseCase {
    public operator fun invoke(companies: List<Company>, query: String): List<Company> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return companies

        return companies.filter { company ->
            company.name.lowercase().contains(trimmed) ||
                company.industry?.lowercase()?.contains(trimmed) == true ||
                company.aliases.any { it.lowercase().contains(trimmed) } ||
                company.metrics?.locations?.any { it.lowercase().contains(trimmed) } == true
        }
    }
}
