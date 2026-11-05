package dev.fnc314.tools.jobtracker.core.data.source

import dev.fnc314.tools.jobtracker.core.data.settings.SettingsStorage

/**
 * Key-value local storage abstraction for application settings and lightweight preferences.
 */
public interface LocalDataSource {
    /**
     * Reads a stored string preference.
     *
     * @param key Preference key.
     * @return Stored value or null if unset.
     */
    public fun getString(key: String): String?

    /**
     * Persists a string preference.
     *
     * @param key Preference key.
     * @param value Value to store.
     */
    public fun putString(key: String, value: String): Unit

    /**
     * Removes a stored preference by key.
     *
     * @param key Preference key.
     */
    public fun remove(key: String): Unit

    /**
     * Clears all stored local preferences.
     */
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
