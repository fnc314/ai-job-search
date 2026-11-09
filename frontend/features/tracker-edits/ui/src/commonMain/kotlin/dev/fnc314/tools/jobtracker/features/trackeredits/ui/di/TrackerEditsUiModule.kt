package dev.fnc314.tools.jobtracker.features.trackeredits.ui.di

import dev.fnc314.tools.jobtracker.features.trackeredits.data.di.trackerEditsDataModule
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.di.trackerEditsDomainModule
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.TrackerEditsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for the tracker edits feature.
 */
public val trackerEditsUiModule: Module = module {
    viewModelOf(::TrackerEditsViewModel)
}

/**
 * Aggregate Koin module uniting data, domain, and UI definitions for the tracker edits feature.
 */
public val trackerEditsFeatureModule: Module = module {
    includes(trackerEditsDataModule, trackerEditsDomainModule, trackerEditsUiModule)
}
