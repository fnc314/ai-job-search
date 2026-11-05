package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import dev.fnc314.tools.jobtracker.features.trackeredits.data.trackerEditsDataModule
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val trackerEditsUiModule = module {
    factoryOf(::TrackerEditsViewModel)
}

val trackerEditsFeatureModule = module {
    includes(trackerEditsDataModule, trackerEditsUiModule)
}
