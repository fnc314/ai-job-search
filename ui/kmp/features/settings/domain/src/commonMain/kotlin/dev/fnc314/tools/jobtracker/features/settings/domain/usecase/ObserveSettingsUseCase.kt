package dev.fnc314.tools.jobtracker.features.settings.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveSettingsUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): StateFlow<AppSettings> = repository.settings
}
