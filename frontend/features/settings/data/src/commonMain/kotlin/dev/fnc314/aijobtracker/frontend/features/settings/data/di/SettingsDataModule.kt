package dev.fnc314.aijobtracker.frontend.features.settings.data.di

import dev.fnc314.aijobtracker.frontend.features.settings.data.repository.impl.SettingsRepositoryImpl
import dev.fnc314.aijobtracker.frontend.features.settings.data.source.LocalSettingsDataSource
import dev.fnc314.aijobtracker.frontend.features.settings.data.source.impl.LocalSettingsDataSourceImpl
import dev.fnc314.aijobtracker.frontend.features.settings.domain.repository.SettingsRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Koin dependency injection module providing settings data sources and repositories. */
public val settingsDataModule: Module = module {
    singleOf(::LocalSettingsDataSourceImpl) bind LocalSettingsDataSource::class
    single<SettingsRepository> { SettingsRepositoryImpl(get(), getOrNull()) }
}
