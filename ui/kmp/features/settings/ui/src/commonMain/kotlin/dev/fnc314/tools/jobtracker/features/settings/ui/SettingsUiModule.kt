package dev.fnc314.tools.jobtracker.features.settings.ui

import dev.fnc314.tools.jobtracker.features.settings.data.settingsDataModule
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val settingsUiModule = module {
    factoryOf(::SettingsViewModel)
}

val settingsFeatureModule = module {
    includes(settingsDataModule, settingsUiModule)
}
