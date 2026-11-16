package dev.fnc314.aijobtracker.frontend.features.settings.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettings

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
