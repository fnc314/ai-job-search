package dev.fnc314.tools.jobtracker.core.data.source

public interface RemoteRecord {
    public val id: String
    public val fields: Map<String, Any?>
    public fun getString(key: String): String?
    public fun getLong(key: String): Long?
    public fun getBoolean(key: String): Boolean?
}
