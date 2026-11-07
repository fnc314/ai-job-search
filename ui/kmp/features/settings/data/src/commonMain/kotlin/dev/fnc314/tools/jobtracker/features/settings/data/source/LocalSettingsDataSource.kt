package dev.fnc314.tools.jobtracker.features.settings.data.source

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings

/**
 * Data source contract for persisting and retrieving user [AppSettings].
 */
public interface LocalSettingsDataSource {
    /**
     * Retrieves stored [AppSettings].
     */
    public fun getSettings(): AppSettings

    /**
     * Saves [settings].
     */
    public fun saveSettings(settings: AppSettings): Unit
}
