package dev.fnc314.tools.jobtracker.features.settings.data

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.settings.SettingsStorage
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(
    private val storage: SettingsStorage,
) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _settings = MutableStateFlow(loadInitialSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadInitialSettings(): AppSettings {
        return try {
            val raw = storage.getString(KEY_SETTINGS)
            if (!raw.isNullOrBlank()) {
                json.decodeFromString<AppSettings>(raw)
            } else {
                AppSettings()
            }
        } catch (_: Exception) {
            AppSettings()
        }
    }

    override fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        try {
            val encoded = json.encodeToString(AppSettings.serializer(), newSettings)
            storage.putString(KEY_SETTINGS, encoded)
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_SETTINGS = "job_tracker_app_settings"
    }
}
