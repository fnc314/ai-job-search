package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import dev.fnc314.tools.jobtracker.features.trackeredits.data.trackerEditsDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

public val trackerEditsUiModule: Module = module {
    factoryOf(::TrackerEditsViewModel)
}

public val trackerEditsFeatureModule: Module = module {
    includes(trackerEditsDataModule, trackerEditsUiModule)
}
