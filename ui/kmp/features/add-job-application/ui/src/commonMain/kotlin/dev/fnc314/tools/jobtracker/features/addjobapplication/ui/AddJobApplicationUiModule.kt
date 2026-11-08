package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import dev.fnc314.tools.jobtracker.features.addjobapplication.data.addJobApplicationDataModule
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.di.addJobApplicationDomainModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for the add job application feature.
 */
public val addJobApplicationUiModule: Module = module {
    viewModelOf(::AddJobApplicationViewModel)
    viewModelOf(::AddLeadViewModel)
}

/**
 * Aggregate Koin module uniting data, domain, and UI definitions for the add job application feature.
 */
public val addJobApplicationFeatureModule: Module = module {
    includes(addJobApplicationDataModule, addJobApplicationDomainModule, addJobApplicationUiModule)
}
