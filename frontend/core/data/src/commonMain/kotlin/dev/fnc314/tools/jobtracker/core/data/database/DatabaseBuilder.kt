package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.RoomDatabase

/**
 * Platform-specific factory function resolving the [RoomDatabase.Builder] for [AppDatabase].
 *
 * Implemented across Android (with Context file paths), Desktop JVM (user home directory), iOS (sandbox document directory),
 * and Web (Wasm/JS memory or worker drivers).
 *
 * @return The preconfigured [RoomDatabase.Builder] or null if the platform environment does not support SQLite.
 */
public expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>?

/**
 * Safe initializer building and opening the [AppDatabase] instance.
 *
 * Catches initialization errors (such as missing native driver symbols or unsupported web workers)
 * and returns null without crashing the application host.
 *
 * @return The operational [AppDatabase] instance, or null if initialization fails.
 */
public fun createRoomDatabase(): AppDatabase? {
  return runCatching {
    getDatabaseBuilder()?.build()
  }.getOrNull()
}
