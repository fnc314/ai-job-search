package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import dev.fnc314.tools.jobtracker.core.data.ApplicationContextHolder

public actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>? {
    val context = ApplicationContextHolder.application ?: return null
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("job_tracker.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
