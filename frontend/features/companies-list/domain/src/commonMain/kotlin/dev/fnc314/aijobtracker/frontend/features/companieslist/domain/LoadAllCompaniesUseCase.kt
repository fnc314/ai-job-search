package dev.fnc314.aijobtracker.frontend.features.companieslist.domain

import dev.fnc314.aijobtracker.frontend.core.data.model.Company
import dev.fnc314.aijobtracker.frontend.features.companieslist.data.repository.CompaniesListRepository

/**
 * Domain usecase that retrieves all employer profiles across tracked applications.
 */
public class LoadAllCompaniesUseCase(
    private val repository: CompaniesListRepository,
) {
    /**
     * Executes the use case to fetch all companies.
     *
     * @param forceRefresh Whether to bypass local cache and query remote storage.
     * @return List of all tracked [Company] records.
     */
    public suspend operator fun invoke(forceRefresh: Boolean = false): List<Company> {
        return repository(forceRefresh)
    }
}
