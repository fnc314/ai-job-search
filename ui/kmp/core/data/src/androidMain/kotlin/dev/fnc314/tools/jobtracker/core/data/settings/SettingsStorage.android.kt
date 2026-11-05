package dev.fnc314.tools.jobtracker.core.data.settings

import android.content.Context
import dev.fnc314.tools.jobtracker.core.data.ApplicationContextHolder

public actual fun createSettingsStorage(): SettingsStorage {
    val context = ApplicationContextHolder.application
    return if (context != null) {
        AndroidSettingsStorage(context)
    } else {
        InMemorySettingsStorage()
    }
}

private class AndroidSettingsStorage(private val context: Context) : SettingsStorage {
    private val prefs by lazy {
        context.getSharedPreferences("job_tracker_prefs", Context.MODE_PRIVATE)
    }

    override fun getString(key: String): String? {
        return prefs.getString(key, null)
    }

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}

private class InMemorySettingsStorage : SettingsStorage {
    private val map = mutableMapOf<String, String>()
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) { map[key] = value }
}
