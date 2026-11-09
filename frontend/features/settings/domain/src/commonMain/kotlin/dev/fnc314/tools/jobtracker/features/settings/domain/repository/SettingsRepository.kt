package dev.fnc314.tools.jobtracker.features.settings.domain.repository

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository contract observing and mutating user application settings.
 */
public interface SettingsRepository {

    /**
     * Hot observable [StateFlow] emitting the latest persisted [AppSettings].
     */
    public val settings: StateFlow<AppSettings>

    /**
     * Updates and saves modified application preferences.
     *
     * @param newSettings Updated [AppSettings] configuration.
     */
    public fun updateSettings(newSettings: AppSettings): Unit
}
