package dev.fnc314.tools.jobtracker.features.settings.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository

public class UpdateSettingsUseCase(
    private val repository: SettingsRepository,
) {
    public operator fun invoke(settings: AppSettings): Unit {
        repository.updateSettings(settings)
    }
}
