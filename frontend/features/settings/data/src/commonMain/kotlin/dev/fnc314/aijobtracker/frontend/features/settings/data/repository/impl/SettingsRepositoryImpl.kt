package dev.fnc314.aijobtracker.frontend.features.settings.data.repository.impl

import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettings
import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettingsRepository
import dev.fnc314.aijobtracker.frontend.features.settings.data.source.LocalSettingsDataSource
import dev.fnc314.aijobtracker.frontend.features.settings.domain.repository.SettingsRepository
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

