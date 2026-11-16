package dev.fnc314.aijobtracker.frontend.core.data.source.impl

import dev.fnc314.aijobtracker.frontend.core.data.settings.SettingsStorage
import dev.fnc314.aijobtracker.frontend.core.data.source.LocalDataSource

internal class LocalDataSourceImpl(
    private val storage: SettingsStorage,
) : LocalDataSource {
    override fun getString(key: String): String? = storage.getString(key)

    override fun putString(key: String, value: String): Unit {
        storage.putString(key, value)
    }

    override fun remove(key: String): Unit {
        storage.putString(key, "")
    }

    override fun clear(): Unit = Unit
}
