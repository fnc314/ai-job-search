package dev.fnc314.aijobtracker.frontend.features.companieslist.ui.di

import dev.fnc314.aijobtracker.frontend.features.companieslist.data.di.companiesListDataModule
import dev.fnc314.aijobtracker.frontend.features.companieslist.domain.di.companiesListDomainModule
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.CompaniesEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.CompaniesListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val companiesListUiModule: Module = module {
    viewModelOf(::CompaniesListViewModel)
    singleOf(::CompaniesEntryRegistry)
}

/**
 * Composite Koin feature module bundling companies list data, domain, and UI dependencies.
 */
public val companiesListFeatureModule: Module = module {
    includes(companiesListDataModule, companiesListDomainModule, companiesListUiModule)
}
