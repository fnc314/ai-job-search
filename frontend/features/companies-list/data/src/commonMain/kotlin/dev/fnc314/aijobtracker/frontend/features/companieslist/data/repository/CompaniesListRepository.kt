package dev.fnc314.aijobtracker.frontend.features.companieslist.data.repository

import dev.fnc314.aijobtracker.frontend.core.data.model.Company

/**
 * Domain repository querying the collection of employers for the companies list experience.
 */
public interface CompaniesListRepository {
    /**
     * Retrieves all companies, utilizing cache when available unless [forceRefresh] is true.
     *
     * @param forceRefresh When true, fetches fresh data from remote and updates local cache.
     * @return List of all known [Company] domain entities.
     */
    public suspend operator fun invoke(forceRefresh: Boolean = false): List<Company>
}
