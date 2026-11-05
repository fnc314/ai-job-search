package dev.fnc314.tools.jobtracker.core.data.di

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseHolder
import dev.fnc314.tools.jobtracker.core.data.database.createRoomDatabase
import dev.fnc314.tools.jobtracker.core.data.json.globalJson
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileOpener
import dev.fnc314.tools.jobtracker.core.data.settings.SettingsStorage
import dev.fnc314.tools.jobtracker.core.data.settings.createSettingsStorage
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSourceImpl
import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSource
import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSourceImpl
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

public val coreDataModule: Module = module {
    single<Json> { globalJson }
    single<SettingsStorage> { createSettingsStorage() }
    single { PlatformFileOpener() }
    single { DatabaseHolder(createRoomDatabase()) }
    single<LocalDataSource> { LocalDataSourceImpl(get()) }
    single<NetworkDataSource> { NetworkDataSourceImpl(get()) }
}
