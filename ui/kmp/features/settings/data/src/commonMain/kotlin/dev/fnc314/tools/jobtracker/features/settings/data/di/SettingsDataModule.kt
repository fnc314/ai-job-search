package dev.fnc314.tools.jobtracker.features.settings.data.di

import dev.fnc314.tools.jobtracker.features.settings.data.repository.impl.SettingsRepositoryImpl
import dev.fnc314.tools.jobtracker.features.settings.data.source.LocalSettingsDataSource
import dev.fnc314.tools.jobtracker.features.settings.data.source.impl.LocalSettingsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val settingsDataModule: Module = module {
    singleOf(::LocalSettingsDataSourceImpl) bind LocalSettingsDataSource::class
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class
}
