package dev.fnc314.tools.jobtracker.features.companieslist.data.di

import dev.fnc314.tools.jobtracker.features.companieslist.data.repository.CompaniesListRepository
import dev.fnc314.tools.jobtracker.features.companieslist.data.repository.impl.CompaniesListRepositoryImpl
import dev.fnc314.tools.jobtracker.features.companieslist.data.source.LocalCompaniesListDataSource
import dev.fnc314.tools.jobtracker.features.companieslist.data.source.RemoteCompaniesListDataSource
import dev.fnc314.tools.jobtracker.features.companieslist.data.source.impl.LocalCompaniesListDataSourceImpl
import dev.fnc314.tools.jobtracker.features.companieslist.data.source.impl.RemoteCompaniesListDataSourceImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val companiesListDataModule: Module = module {
    singleOf(::RemoteCompaniesListDataSourceImpl) bind RemoteCompaniesListDataSource::class
    singleOf(::LocalCompaniesListDataSourceImpl) bind LocalCompaniesListDataSource::class
    singleOf(::CompaniesListRepositoryImpl) bind CompaniesListRepository::class
}
