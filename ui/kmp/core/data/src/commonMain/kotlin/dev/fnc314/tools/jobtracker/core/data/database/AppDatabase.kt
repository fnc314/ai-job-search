package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import dev.fnc314.tools.jobtracker.core.data.database.dao.JobApplicationDao
import dev.fnc314.tools.jobtracker.core.data.database.entity.JobApplicationEntity

/**
 * Multiplatform Room 3 embedded database providing local relational caching and offline-first queries.
 *
 * Configured with [JobApplicationEntity] schema tables and registered type converters [DatabaseConverters].
 * Initialized across Android, Desktop JVM, iOS, and Web via expect/actual [DatabaseBuilder].
 */
@Database(entities = [JobApplicationEntity::class], version = 1)
@ColumnTypeConverters(DatabaseConverters::class)
public abstract class AppDatabase : RoomDatabase() {

    /**
     * Data Access Object exposing relational queries for job applications.
     *
     * @return The [JobApplicationDao] instance.
     */
    public abstract fun jobApplicationDao(): JobApplicationDao
}
