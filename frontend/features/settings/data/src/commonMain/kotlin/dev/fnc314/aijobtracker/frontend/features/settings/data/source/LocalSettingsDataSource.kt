package dev.fnc314.aijobtracker.frontend.features.settings.data.source

import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettings

/**
 * Data source contract for persisting and retrieving user [AppSettings].
 */
internal interface LocalSettingsDataSource {
    /**
     * Retrieves stored [AppSettings].
     */
    public fun getSettings(): AppSettings

    /**
     * Saves [settings].
     */
    public fun saveSettings(settings: AppSettings): Unit
}
