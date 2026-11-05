package dev.fnc314.tools.jobtracker.core.data.source

import dev.fnc314.tools.jobtracker.core.data.settings.SettingsStorage

public interface LocalDataSource {
    public fun getString(key: String): String?
    public fun putString(key: String, value: String): Unit
    public fun remove(key: String): Unit
    public fun clear(): Unit
}

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

    override fun clear(): Unit {}
}
