package dev.fnc314.tools.jobtracker.features.companieslist.domain

import dev.fnc314.tools.jobtracker.core.data.model.Company

public enum class CompanySortOption(public val displayName: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    APPLICATIONS_COUNT_DESC("Applications (Most)"),
    APPLICATIONS_COUNT_ASC("Applications (Least)"),
    FIT_SCORE_DESC("Fit Score (Highest)"),
    FIT_SCORE_ASC("Fit Score (Lowest)"),
}

public class SortCompaniesUseCase {
    public operator fun invoke(
        companies: List<Company>,
        sortOption: CompanySortOption = CompanySortOption.NAME_ASC,
    ): List<Company> {
        return when (sortOption) {
            CompanySortOption.NAME_ASC -> companies.sortedBy { it.name.lowercase() }
            CompanySortOption.NAME_DESC -> companies.sortedByDescending { it.name.lowercase() }
            CompanySortOption.APPLICATIONS_COUNT_DESC -> companies.sortedByDescending { it.metrics?.counts?.total ?: 0 }
            CompanySortOption.APPLICATIONS_COUNT_ASC -> companies.sortedBy { it.metrics?.counts?.total ?: 0 }
            CompanySortOption.FIT_SCORE_DESC -> companies.sortedByDescending { it.metrics?.averageFitScore ?: 0.0 }
            CompanySortOption.FIT_SCORE_ASC -> companies.sortedBy { it.metrics?.averageFitScore ?: 0.0 }
        }
    }
}
