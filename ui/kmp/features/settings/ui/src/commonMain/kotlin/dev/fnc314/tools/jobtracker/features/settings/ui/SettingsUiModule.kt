package dev.fnc314.tools.jobtracker.features.settings.ui

import dev.fnc314.tools.jobtracker.features.settings.data.settingsDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for the settings feature.
 */
public val settingsUiModule: Module = module {
    factoryOf(::SettingsViewModel)
}

/**
 * Aggregate Koin module uniting data and UI definitions for application settings.
 */
public val settingsFeatureModule: Module = module {
    includes(settingsDataModule, settingsUiModule)
}
