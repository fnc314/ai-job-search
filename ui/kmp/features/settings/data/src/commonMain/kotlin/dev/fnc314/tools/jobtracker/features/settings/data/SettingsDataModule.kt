package dev.fnc314.tools.jobtracker.features.settings.data

import dev.fnc314.tools.jobtracker.features.settings.data.source.LocalSettingsDataSource
import dev.fnc314.tools.jobtracker.features.settings.data.source.LocalSettingsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.ObserveSettingsUseCase
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.UpdateSettingsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val settingsDataModule: Module = module {
    singleOf(::LocalSettingsDataSourceImpl) bind LocalSettingsDataSource::class
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class
    factoryOf(::ObserveSettingsUseCase)
    factoryOf(::UpdateSettingsUseCase)
}
