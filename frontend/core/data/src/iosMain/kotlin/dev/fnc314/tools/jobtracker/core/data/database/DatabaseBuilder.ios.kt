package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
public actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>? {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val dbFilePath = "${documentDirectory?.path}/job_tracker.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
    ).setDriver(BundledSQLiteDriver())
}
