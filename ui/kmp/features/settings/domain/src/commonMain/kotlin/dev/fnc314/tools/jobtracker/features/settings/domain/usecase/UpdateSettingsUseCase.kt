package dev.fnc314.tools.jobtracker.features.settings.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository

/**
 * Clean architecture use case saving modified user preferences.
 *
 * @param repository The backing [SettingsRepository].
 */
public class UpdateSettingsUseCase(
    private val repository: SettingsRepository,
) {
    /**
     * Updates and persists [settings].
     *
     * @param settings The new [AppSettings] configuration to save.
     */
    public operator fun invoke(settings: AppSettings): Unit {
        repository.updateSettings(settings)
    }
}
