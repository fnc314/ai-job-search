package dev.fnc314.tools.jobtracker.features.settings.data

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.ObserveSettingsUseCase
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.UpdateSettingsUseCase
import org.koin.dsl.module

val settingsDataModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single { AppSettingsRepository(get()) }
    factory { ObserveSettingsUseCase(get()) }
    factory { UpdateSettingsUseCase(get()) }
}
