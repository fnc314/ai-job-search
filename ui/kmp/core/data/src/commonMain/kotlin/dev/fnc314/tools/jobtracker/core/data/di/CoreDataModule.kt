package dev.fnc314.tools.jobtracker.core.data.di

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseHolder
import dev.fnc314.tools.jobtracker.core.data.database.createRoomDatabase
import dev.fnc314.tools.jobtracker.core.data.json.globalJson
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileOpener
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.core.data.settings.SettingsStorage
import dev.fnc314.tools.jobtracker.core.data.settings.createSettingsStorage
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSourceImpl
import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSource
import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSourceImpl
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.core.data.source.createRemoteDataSource
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
 * - [DatabaseHolder] ([createRoomDatabase])
 * - [LocalDataSource] ([LocalDataSourceImpl])
 * - [NetworkDataSource] ([NetworkDataSourceImpl])
 */
public val coreDataModule: Module = module {
    single<Json> { globalJson }
    single<SettingsStorage> { createSettingsStorage() }
    singleOf(::AppSettingsRepository)
    single { PlatformFileOpener() }
    single { DatabaseHolder(createRoomDatabase()) }
    single<LocalDataSource> { LocalDataSourceImpl(get()) }
    single<NetworkDataSource> { NetworkDataSourceImpl(get()) }
    single<RemoteDataSource> { createRemoteDataSource() }
}
