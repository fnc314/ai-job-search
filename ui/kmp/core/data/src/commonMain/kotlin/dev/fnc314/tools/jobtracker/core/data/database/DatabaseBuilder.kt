package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.RoomDatabase

public expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>?

public fun createRoomDatabase(): AppDatabase? {
    return runCatching {
        getDatabaseBuilder()?.build()
    }.getOrNull()
}
