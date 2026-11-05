package dev.fnc314.tools.jobtracker.features.settings.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository

class UpdateSettingsUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(settings: AppSettings) {
        repository.updateSettings(settings)
    }
}
