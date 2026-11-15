package dev.fnc314.tools.jobtracker.features.companieslist.data.source

import dev.fnc314.tools.jobtracker.core.data.model.Company

/**
 * Local cache contract for companies list.
 */
internal interface LocalCompaniesListDataSource {
    suspend fun getCompanies(): List<Company>
    suspend fun saveCompanies(companies: List<Company>)
    suspend fun clearAndSaveCompanies(companies: List<Company>)
}
