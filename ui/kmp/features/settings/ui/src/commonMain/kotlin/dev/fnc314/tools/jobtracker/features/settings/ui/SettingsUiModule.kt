package dev.fnc314.tools.jobtracker.features.settings.ui

import dev.fnc314.tools.jobtracker.features.settings.data.settingsDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

public val settingsUiModule: Module = module {
    factoryOf(::SettingsViewModel)
}

public val settingsFeatureModule: Module = module {
    includes(settingsDataModule, settingsUiModule)
}
