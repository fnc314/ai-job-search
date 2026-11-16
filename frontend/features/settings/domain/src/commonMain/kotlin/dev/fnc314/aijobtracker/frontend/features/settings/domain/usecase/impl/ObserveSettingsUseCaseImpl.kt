package dev.fnc314.aijobtracker.frontend.features.settings.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettings
import dev.fnc314.aijobtracker.frontend.features.settings.domain.repository.SettingsRepository
import dev.fnc314.aijobtracker.frontend.features.settings.domain.usecase.ObserveSettingsUseCase
import kotlinx.coroutines.flow.StateFlow

internal class ObserveSettingsUseCaseImpl(
    private val repository: SettingsRepository,
) : ObserveSettingsUseCase {
    override operator fun invoke(): StateFlow<AppSettings> = repository.settings
}
