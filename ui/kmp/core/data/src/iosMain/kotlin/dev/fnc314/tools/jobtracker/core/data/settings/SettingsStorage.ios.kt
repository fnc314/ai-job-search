package dev.fnc314.tools.jobtracker.core.data.settings

import platform.Foundation.NSUserDefaults

public actual fun createSettingsStorage(): SettingsStorage = IosSettingsStorage()

private class IosSettingsStorage : SettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? {
        return defaults.stringForKey(key)
    }

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
