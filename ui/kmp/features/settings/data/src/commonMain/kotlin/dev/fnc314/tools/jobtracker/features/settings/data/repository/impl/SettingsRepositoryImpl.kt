package dev.fnc314.tools.jobtracker.features.settings.data.repository.impl

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.features.settings.data.source.LocalSettingsDataSource
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

internal class SettingsRepositoryImpl(
    private val appSettingsRepository: AppSettingsRepository,
    private val localDataSource: LocalSettingsDataSource? = null,
) : SettingsRepository {
    override val settings: StateFlow<AppSettings> = appSettingsRepository.settings

    override fun updateSettings(newSettings: AppSettings): Unit {
        appSettingsRepository.updateSettings(newSettings)
        localDataSource?.saveSettings(newSettings)
    }
}

