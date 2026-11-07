package dev.fnc314.tools.jobtracker.features.settings.data.repository.impl

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.features.settings.data.source.LocalSettingsDataSource
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SettingsRepositoryImpl(
    private val localDataSource: LocalSettingsDataSource,
) : SettingsRepository {
    private val _settings = MutableStateFlow(localDataSource.getSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun updateSettings(newSettings: AppSettings): Unit {
        _settings.value = newSettings
        localDataSource.saveSettings(newSettings)
    }
}
