package dev.fnc314.tools.jobtracker.core.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Key-value persistent storage contract abstracting platform-specific settings stores.
 */
public interface SettingsStorage {
    /**
     * Reads a stored string by key.
     *
     * @param key Storage key.
     * @return Value or null if missing.
     */
    public fun getString(key: String): String?

    /**
     * Writes a string key-value pair.
     *
     * @param key Storage key.
     * @param value String value to persist.
     */
    public fun putString(key: String, value: String): Unit
}

/**
 * Multiplatform factory resolving the platform-native [SettingsStorage] implementation.
 *
 * Implemented via SharedPreferences on Android, Preferences API on Desktop JVM, NSUserDefaults on iOS,
 * and LocalStorage on Web.
 *
 * @return Platform [SettingsStorage] instance.
 */
public expect fun createSettingsStorage(): SettingsStorage

/**
 * Reactive settings repository serializing [AppSettings] to [SettingsStorage] and exposing reactive [StateFlow].
 *
 * @param storage The underlying platform storage instance.
 */
public class AppSettingsRepository(
    private val storage: SettingsStorage,
) {
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _settings: MutableStateFlow<AppSettings> = MutableStateFlow(loadInitialSettings())

    /**
     * Hot [StateFlow] emitting the current user preferences.
     */
    public val settings: StateFlow<AppSettings> = _settings.asStateFlow()

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

    /**
     * Updates and persists new application preferences.
     *
     * @param newSettings Updated [AppSettings] configuration.
     */
    public fun updateSettings(newSettings: AppSettings): Unit {
        _settings.value = newSettings
        try {
            val encoded = json.encodeToString(AppSettings.serializer(), newSettings)
            storage.putString(KEY_SETTINGS, encoded)
        } catch (_: Exception) {}
    }

    public companion object {
        private const val KEY_SETTINGS: String = "job_tracker_app_settings"
    }
}
