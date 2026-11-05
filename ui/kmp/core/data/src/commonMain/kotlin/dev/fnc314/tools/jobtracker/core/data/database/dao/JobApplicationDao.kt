package dev.fnc314.tools.jobtracker.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import dev.fnc314.tools.jobtracker.core.data.database.entity.JobApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface JobApplicationDao {
    @Query("SELECT * FROM job_applications")
    public fun observeAll(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE id = :id")
    public suspend fun getById(id: String): JobApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAll(applications: List<JobApplicationEntity>): Unit

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(application: JobApplicationEntity): Unit

    @Query("DELETE FROM job_applications WHERE id = :id")
    public suspend fun deleteById(id: String): Unit
}
