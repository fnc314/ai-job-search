package dev.fnc314.tools.jobtracker.features.companieslist.data.repository.impl

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.features.companieslist.data.repository.CompaniesListRepository
import dev.fnc314.tools.jobtracker.features.companieslist.data.source.LocalCompaniesListDataSource
import dev.fnc314.tools.jobtracker.features.companieslist.data.source.RemoteCompaniesListDataSource

internal class CompaniesListRepositoryImpl(
    private val localDataSource: LocalCompaniesListDataSource,
    private val remoteDataSource: RemoteCompaniesListDataSource,
) : CompaniesListRepository {

    override suspend operator fun invoke(forceRefresh: Boolean): List<Company> {
        if (!forceRefresh) {
            val cached = localDataSource.getCompanies()
            if (cached.isNotEmpty()) {
                return cached
            }
        }

        val remoteCompanies = remoteDataSource()
        localDataSource.clearAndSaveCompanies(remoteCompanies)
        return remoteCompanies
    }
}
