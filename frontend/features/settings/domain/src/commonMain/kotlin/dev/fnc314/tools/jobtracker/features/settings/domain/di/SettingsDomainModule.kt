package dev.fnc314.tools.jobtracker.features.settings.domain.di

import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.ObserveSettingsUseCase
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.UpdateSettingsUseCase
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.impl.ObserveSettingsUseCaseImpl
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.impl.UpdateSettingsUseCaseImpl
import org.koin.core.module.Module
import org.koin.dsl.module

public val settingsDomainModule: Module = module {
    factory<ObserveSettingsUseCase> { ObserveSettingsUseCaseImpl(get()) }
    factory<UpdateSettingsUseCase> { UpdateSettingsUseCaseImpl(get()) }
}

