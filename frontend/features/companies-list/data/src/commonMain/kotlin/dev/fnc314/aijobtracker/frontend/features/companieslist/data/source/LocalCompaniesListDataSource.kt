package dev.fnc314.aijobtracker.frontend.features.companieslist.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.Company

/**
 * Local cache contract for companies list.
 */
internal interface LocalCompaniesListDataSource {
    suspend fun getCompanies(): List<Company>
    suspend fun saveCompanies(companies: List<Company>)
    suspend fun clearAndSaveCompanies(companies: List<Company>)
}
