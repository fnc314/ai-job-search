package dev.fnc314.tools.jobtracker.features.settings.domain.repository

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import kotlinx.coroutines.flow.StateFlow

public interface SettingsRepository {
    public val settings: StateFlow<AppSettings>
    public fun updateSettings(newSettings: AppSettings): Unit
}
