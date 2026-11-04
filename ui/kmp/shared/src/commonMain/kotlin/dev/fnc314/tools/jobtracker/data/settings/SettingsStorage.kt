package dev.fnc314.tools.jobtracker.data.settings

import dev.fnc314.tools.jobtracker.ui.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

interface SettingsStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

expect fun createSettingsStorage(): SettingsStorage

class AppSettingsRepository(
    private val storage: SettingsStorage,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _settings = MutableStateFlow(loadInitialSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

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

    fun updateSettings(newSettings: AppSettings) {
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
