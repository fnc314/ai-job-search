package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import dev.fnc314.tools.jobtracker.features.addjobapplication.data.addJobApplicationDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for adding applications and leads.
 */
public val addJobApplicationUiModule: Module = module {
    factoryOf(::AddJobApplicationViewModel)
    factoryOf(::AddLeadViewModel)
}

/**
 * Aggregate Koin module uniting data and UI definitions for the add application and lead feature.
 */
public val addJobApplicationFeatureModule: Module = module {
    includes(addJobApplicationDataModule, addJobApplicationUiModule)
}
