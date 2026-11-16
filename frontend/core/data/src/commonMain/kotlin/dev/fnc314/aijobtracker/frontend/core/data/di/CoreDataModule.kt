package dev.fnc314.aijobtracker.frontend.core.data.di

import dev.fnc314.aijobtracker.frontend.core.data.database.DatabaseHolder
import dev.fnc314.aijobtracker.frontend.core.data.database.createLocalDatabase
import dev.fnc314.aijobtracker.frontend.core.data.json.globalJson
import dev.fnc314.aijobtracker.frontend.core.data.platform.PlatformFileOpener
import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettingsRepository
import dev.fnc314.aijobtracker.frontend.core.data.settings.SettingsStorage
import dev.fnc314.aijobtracker.frontend.core.data.settings.createSettingsStorage
import dev.fnc314.aijobtracker.frontend.core.data.source.LocalDataSource
import dev.fnc314.aijobtracker.frontend.core.data.source.NetworkDataSource
import dev.fnc314.aijobtracker.frontend.core.data.source.RemoteDataSource
import dev.fnc314.aijobtracker.frontend.core.data.source.createRemoteDataSource
import dev.fnc314.aijobtracker.frontend.core.data.source.impl.LocalDataSourceImpl
import dev.fnc314.aijobtracker.frontend.core.data.source.impl.NetworkDataSourceImpl
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin dependency injection module providing foundational core data singletons.
 *
 * Registers:
 * - [Json] ([globalJson])
 * - [SettingsStorage] ([createSettingsStorage])
 * - [AppSettingsRepository] ([AppSettingsRepository])
 * - [PlatformFileOpener]
 * - [DatabaseHolder] ([createLocalDatabase])
 * - [LocalDataSource] ([LocalDataSourceImpl])
 * - [NetworkDataSource] ([NetworkDataSourceImpl])
 */
public val coreDataModule: Module = module {
    single<Json> { globalJson }
    single<SettingsStorage> { createSettingsStorage() }
    singleOf(::AppSettingsRepository)
    single { PlatformFileOpener() }
    single { DatabaseHolder(createLocalDatabase()) }
    single<LocalDataSource> { LocalDataSourceImpl(get()) }
    single<NetworkDataSource> { NetworkDataSourceImpl(get()) }
    single<RemoteDataSource> { createRemoteDataSource() }
}
