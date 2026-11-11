package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

public actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>? {
    val dbFile = File(System.getProperty("user.home"), ".job_tracker/job_tracker.db")
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    ).setDriver(BundledSQLiteDriver())
}

public actual fun attachInspector(database: AppDatabase) {
    // No-op: kmp-inspector-room does not support Room 3 (androidx.room3) yet
}
