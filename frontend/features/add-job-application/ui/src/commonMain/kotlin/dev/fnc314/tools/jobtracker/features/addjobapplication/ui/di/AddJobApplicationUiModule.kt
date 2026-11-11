package dev.fnc314.tools.jobtracker.features.addjobapplication.ui.di

import dev.fnc314.tools.jobtracker.features.addjobapplication.data.di.addJobApplicationDataModule
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.di.addJobApplicationDomainModule
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddJobApplicationEntryRegistry
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddJobApplicationViewModel
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddLeadViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for the add job application feature.
 */
public val addJobApplicationUiModule: Module = module {
    viewModelOf(::AddJobApplicationViewModel)
    viewModelOf(::AddLeadViewModel)
    singleOf(::AddJobApplicationEntryRegistry)
}

/**
 * Aggregate Koin module uniting data, domain, and UI definitions for the add job application feature.
 */
public val addJobApplicationFeatureModule: Module = module {
    includes(addJobApplicationDataModule, addJobApplicationDomainModule, addJobApplicationUiModule)
}
