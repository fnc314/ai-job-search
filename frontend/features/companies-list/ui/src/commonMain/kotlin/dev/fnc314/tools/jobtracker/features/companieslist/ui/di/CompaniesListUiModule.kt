package dev.fnc314.tools.jobtracker.features.companieslist.ui.di

import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.features.companieslist.data.di.companiesListDataModule
import dev.fnc314.tools.jobtracker.features.companieslist.domain.di.companiesListDomainModule
import dev.fnc314.tools.jobtracker.features.companieslist.ui.CompaniesEntryRegistry
import dev.fnc314.tools.jobtracker.features.companieslist.ui.CompaniesListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.single

public val companiesListUiModule: Module = module {
    viewModelOf(::CompaniesListViewModel)
    single<FeatureEntryRegistry> { CompaniesEntryRegistry() }
}

public val companiesListFeatureModule: Module = module {
    includes(companiesListDataModule, companiesListDomainModule, companiesListUiModule)
}
