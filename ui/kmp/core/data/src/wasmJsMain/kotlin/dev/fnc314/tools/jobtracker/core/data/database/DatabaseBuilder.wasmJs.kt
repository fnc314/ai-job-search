package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.RoomDatabase

public actual fun getDatabaseBuilder(addInspector: RoomDatabase.Builder<AppDatabase>.() -> RoomDatabase.Builder<AppDatabase>?): RoomDatabase.Builder<AppDatabase>? {
    return null
}
