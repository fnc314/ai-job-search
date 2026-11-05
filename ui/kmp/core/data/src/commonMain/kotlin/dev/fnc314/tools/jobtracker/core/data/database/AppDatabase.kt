package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import dev.fnc314.tools.jobtracker.core.data.database.dao.JobApplicationDao
import dev.fnc314.tools.jobtracker.core.data.database.entity.JobApplicationEntity

@Database(entities = [JobApplicationEntity::class], version = 1)
public abstract class AppDatabase : RoomDatabase() {
    public abstract fun jobApplicationDao(): JobApplicationDao
}
