package dev.fnc314.tools.jobtracker.features.settings.domain.repository

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun updateSettings(newSettings: AppSettings)
}
