package dev.fnc314.tools.jobtracker.data.settings

import kotlinx.browser.localStorage

actual fun createSettingsStorage(): SettingsStorage = JsSettingsStorage()

private class JsSettingsStorage : SettingsStorage {
    override fun getString(key: String): String? {
        return try {
            localStorage.getItem(key)
        } catch (_: Exception) {
            null
        }
    }

    override fun putString(key: String, value: String) {
        try {
            localStorage.setItem(key, value)
        } catch (_: Exception) {}
    }
}
