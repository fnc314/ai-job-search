package dev.fnc314.tools.jobtracker.features.companieslist.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.Company

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
