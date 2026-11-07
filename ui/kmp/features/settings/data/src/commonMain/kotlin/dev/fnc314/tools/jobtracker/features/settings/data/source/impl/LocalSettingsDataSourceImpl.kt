package dev.fnc314.tools.jobtracker.features.settings.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource
import dev.fnc314.tools.jobtracker.features.settings.data.source.LocalSettingsDataSource
import kotlinx.serialization.json.Json

internal class LocalSettingsDataSourceImpl(
    private val localDataSource: LocalDataSource,
) : LocalSettingsDataSource {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun getSettings(): AppSettings {
        return try {
            val raw = localDataSource.getString(KEY_SETTINGS)
            if (!raw.isNullOrBlank()) {
                json.decodeFromString<AppSettings>(raw)
            } else {
                AppSettings()
            }
        } catch (_: Exception) {
            AppSettings()
        }
    }

    override fun saveSettings(settings: AppSettings): Unit {
        try {
            val encoded = json.encodeToString(AppSettings.serializer(), settings)
            localDataSource.putString(KEY_SETTINGS, encoded)
        } catch (_: Exception) {}
    }

    private companion object {
        private const val KEY_SETTINGS = "job_tracker_app_settings"
    }
}
