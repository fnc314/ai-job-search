package dev.fnc314.aijobtracker.frontend.core.data.database

import androidx.room3.RoomDatabase

public actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>? {
    return null
}

public actual fun attachInspector(database: AppDatabase) {
    // No-op: kmp-inspector does not support web targets
}
