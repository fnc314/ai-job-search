package dev.fnc314.tools.jobtracker.core.data.source

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

