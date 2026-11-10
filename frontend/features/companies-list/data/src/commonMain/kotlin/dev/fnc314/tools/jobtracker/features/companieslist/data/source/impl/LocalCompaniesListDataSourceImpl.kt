package dev.fnc314.tools.jobtracker.features.companieslist.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.features.companieslist.data.source.LocalCompaniesListDataSource

internal class LocalCompaniesListDataSourceImpl : LocalCompaniesListDataSource {
    private val memoryCache = mutableListOf<Company>()

    override suspend fun getCompanies(): List<Company> {
        return memoryCache.toList()
    }

    override suspend fun saveCompanies(companies: List<Company>) {
        memoryCache.clear()
        memoryCache.addAll(companies)
    }

    override suspend fun clearAndSaveCompanies(companies: List<Company>) {
        memoryCache.clear()
        memoryCache.addAll(companies)
    }
}
