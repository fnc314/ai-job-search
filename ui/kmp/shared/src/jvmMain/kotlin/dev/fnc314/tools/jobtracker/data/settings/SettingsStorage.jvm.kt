package dev.fnc314.tools.jobtracker.data.settings

import java.util.prefs.Preferences

actual fun createSettingsStorage(): SettingsStorage = JvmSettingsStorage()

private class JvmSettingsStorage : SettingsStorage {
    private val prefs: Preferences by lazy {
        Preferences.userRoot().node("dev/fnc314/tools/jobtracker")
    }

    override fun getString(key: String): String? {
        return prefs.get(key, null)
    }

    override fun putString(key: String, value: String) {
        prefs.put(key, value)
        try {
            prefs.flush()
        } catch (_: Exception) {}
    }
}
