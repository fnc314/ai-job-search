package dev.fnc314.tools.jobtracker.features.settings.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings

/**
 * Clean architecture use case contract saving modified user preferences.
 */
public fun interface UpdateSettingsUseCase {
    /**
     * Updates and persists [settings].
     *
     * @param settings The new [AppSettings] configuration to save.
     */
    public operator fun invoke(settings: AppSettings): Unit
}
