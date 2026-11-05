package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import dev.fnc314.tools.jobtracker.features.trackeredits.data.trackerEditsDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for reviewing proposed tracker edits.
 */
public val trackerEditsUiModule: Module = module {
    factoryOf(::TrackerEditsViewModel)
}

/**
 * Aggregate Koin module uniting data and UI definitions for the tracker edits feature.
 */
public val trackerEditsFeatureModule: Module = module {
    includes(trackerEditsDataModule, trackerEditsUiModule)
}
