package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import dev.fnc314.tools.jobtracker.features.addjobapplication.data.addJobApplicationDataModule
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

public val addJobApplicationUiModule: org.koin.core.module.Module = module {
    factoryOf(::AddJobApplicationViewModel)
    factoryOf(::AddLeadViewModel)
}

public val addJobApplicationFeatureModule: org.koin.core.module.Module = module {
    includes(addJobApplicationDataModule, addJobApplicationUiModule)
}
