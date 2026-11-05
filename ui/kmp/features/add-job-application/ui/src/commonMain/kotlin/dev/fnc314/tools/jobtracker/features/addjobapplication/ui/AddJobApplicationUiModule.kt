package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import dev.fnc314.tools.jobtracker.features.addjobapplication.data.addJobApplicationDataModule
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val addJobApplicationUiModule = module {
    factoryOf(::AddJobApplicationViewModel)
    factoryOf(::AddLeadViewModel)
}

val addJobApplicationFeatureModule = module {
    includes(addJobApplicationDataModule, addJobApplicationUiModule)
}
