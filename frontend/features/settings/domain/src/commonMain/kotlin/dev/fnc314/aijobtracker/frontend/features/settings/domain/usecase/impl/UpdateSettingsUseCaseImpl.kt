package dev.fnc314.aijobtracker.frontend.features.settings.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettings
import dev.fnc314.aijobtracker.frontend.features.settings.domain.repository.SettingsRepository
import dev.fnc314.aijobtracker.frontend.features.settings.domain.usecase.UpdateSettingsUseCase

internal class UpdateSettingsUseCaseImpl(
    private val repository: SettingsRepository,
) : UpdateSettingsUseCase {
    override operator fun invoke(settings: AppSettings): Unit {
        repository.updateSettings(settings)
    }
}
