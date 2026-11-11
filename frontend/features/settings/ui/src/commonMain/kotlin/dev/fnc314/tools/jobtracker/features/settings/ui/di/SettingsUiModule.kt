package dev.fnc314.tools.jobtracker.features.settings.ui.di

import dev.fnc314.tools.jobtracker.features.settings.data.di.settingsDataModule
import dev.fnc314.tools.jobtracker.features.settings.domain.di.settingsDomainModule
import dev.fnc314.tools.jobtracker.features.settings.ui.SettingsEntryRegistry
import dev.fnc314.tools.jobtracker.features.settings.ui.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for the settings feature.
 */
public val settingsUiModule: Module = module {
    viewModelOf(::SettingsViewModel)
    singleOf(::SettingsEntryRegistry)
}

/**
 * Aggregate Koin module uniting data, domain, and UI definitions for application settings.
 */
public val settingsFeatureModule: Module = module {
    includes(settingsDataModule, settingsDomainModule, settingsUiModule)
}
