package dev.fnc314.tools.jobtracker.features.companydetails.ui.di

import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.features.companydetails.data.di.companyDetailsDataModule
import dev.fnc314.tools.jobtracker.features.companydetails.domain.di.companyDetailsDomainModule
import dev.fnc314.tools.jobtracker.features.companydetails.ui.CompanyDetailsEntryRegistry
import dev.fnc314.tools.jobtracker.features.companydetails.ui.CompanyDetailsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.single

public val companyDetailsUiModule: Module = module {
    viewModelOf(::CompanyDetailsViewModel)
    single<FeatureEntryRegistry> { CompanyDetailsEntryRegistry() }
}

public val companyDetailsFeatureModule: Module = module {
    includes(companyDetailsDataModule, companyDetailsDomainModule, companyDetailsUiModule)
}
