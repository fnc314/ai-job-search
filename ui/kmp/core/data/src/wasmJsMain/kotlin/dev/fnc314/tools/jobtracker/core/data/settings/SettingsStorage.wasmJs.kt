package dev.fnc314.tools.jobtracker.core.data.settings

import kotlinx.browser.localStorage

public actual fun createSettingsStorage(): SettingsStorage = WasmJsSettingsStorage()

private class WasmJsSettingsStorage : SettingsStorage {
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
