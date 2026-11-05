package dev.fnc314.tools.jobtracker.core.data.database

import dev.fnc314.tools.jobtracker.core.data.database.dao.JobApplicationDao

public class DatabaseHolder(
    public val database: AppDatabase?,
) {
    public val dao: JobApplicationDao?
        get() = database?.jobApplicationDao()
}
