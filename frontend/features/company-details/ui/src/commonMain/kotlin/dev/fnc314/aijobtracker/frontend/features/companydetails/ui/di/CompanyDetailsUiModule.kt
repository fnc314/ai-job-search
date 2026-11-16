package dev.fnc314.aijobtracker.frontend.features.companydetails.ui.di

import dev.fnc314.aijobtracker.frontend.features.companydetails.data.di.companyDetailsDataModule
import dev.fnc314.aijobtracker.frontend.features.companydetails.domain.di.companyDetailsDomainModule
import dev.fnc314.aijobtracker.frontend.features.companydetails.ui.CompanyDetailsEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.companydetails.ui.CompanyDetailsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val companyDetailsUiModule: Module = module {
    viewModelOf(::CompanyDetailsViewModel)
    singleOf(::CompanyDetailsEntryRegistry)
}

/**
 * Composite Koin feature module bundling company details data, domain, and UI dependencies.
 */
public val companyDetailsFeatureModule: Module = module {
    includes(companyDetailsDataModule, companyDetailsDomainModule, companyDetailsUiModule)
}
