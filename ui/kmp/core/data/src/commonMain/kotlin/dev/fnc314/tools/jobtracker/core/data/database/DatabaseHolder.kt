package dev.fnc314.tools.jobtracker.core.data.database

import dev.fnc314.tools.jobtracker.core.data.database.dao.JobApplicationDao

/**
 * Thread-safe DI container holding a nullable [AppDatabase] instance.
 *
 * Allows non-nullable dependency injection resolution in Koin while safely accommodating
 * platforms or test environments where local database initialization may be skipped or deferred.
 *
 * @property database The underlying [AppDatabase] instance, or null if database storage is unavailable.
 */
public class DatabaseHolder(
    public val database: AppDatabase?,
) {
    /**
     * Exposes the [JobApplicationDao] if the database is initialized, or null otherwise.
     */
    public val dao: JobApplicationDao?
        get() = database?.jobApplicationDao()
}
