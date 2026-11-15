package dev.fnc314.tools.jobtracker.features.companieslist.domain

import dev.fnc314.tools.jobtracker.core.data.model.Company

/**
 * Sorting criteria available for ordering companies in the list.
 *
 * @property displayName Human-readable label for display in UI selectors.
 */
public enum class CompanySortOption(public val displayName: String) {
    /** Alphabetical ordering ascending by employer name. */
    NAME_ASC("Name (A–Z)"),
    /** Alphabetical ordering descending by employer name. */
    NAME_DESC("Name (Z–A)"),
    /** Ordering by highest total application count. */
    APPLICATIONS_COUNT_DESC("Applications (Most)"),
    /** Ordering by lowest total application count. */
    APPLICATIONS_COUNT_ASC("Applications (Least)"),
    /** Ordering by highest average candidate fit score. */
    FIT_SCORE_DESC("Fit Score (Highest)"),
    /** Ordering by lowest average candidate fit score. */
    FIT_SCORE_ASC("Fit Score (Lowest)"),
}

/**
 * Use case that orders a collection of employers based on [CompanySortOption].
 */
public class SortCompaniesUseCase {
    /**
     * Sorts [companies] according to [sortOption].
     *
     * @param companies Collection of employers to sort.
     * @param sortOption Chosen ordering criterion.
     * @return Sorted list of employers.
     */
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
