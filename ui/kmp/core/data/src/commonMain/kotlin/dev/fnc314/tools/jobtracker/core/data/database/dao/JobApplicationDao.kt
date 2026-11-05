package dev.fnc314.tools.jobtracker.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import dev.fnc314.tools.jobtracker.core.data.database.entity.JobApplicationEntity
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlinx.coroutines.flow.Flow

@Dao
public interface JobApplicationDao {
    @Query("SELECT * FROM job_applications ORDER BY createdAt DESC")
    public fun observeAll(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications")
    public suspend fun getAll(): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE uuid = :uuid LIMIT 1")
    public suspend fun getByUuid(uuid: Uuid7): JobApplicationEntity?

    @Query("SELECT * FROM job_applications WHERE slugId = :slugId LIMIT 1")
    public suspend fun getBySlugId(slugId: String): JobApplicationEntity?

    @Query("SELECT * FROM job_applications WHERE slugId = :idOrUuidString OR uuid = :idOrUuidString LIMIT 1")
    public suspend fun getByIdOrUuid(idOrUuidString: String): JobApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAll(applications: List<JobApplicationEntity>): Unit

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(application: JobApplicationEntity): Unit

    @Query("DELETE FROM job_applications WHERE uuid = :uuid")
    public suspend fun deleteByUuid(uuid: Uuid7): Unit

    @Query("DELETE FROM job_applications WHERE slugId = :slugId")
    public suspend fun deleteBySlugId(slugId: String): Unit

    @Query("DELETE FROM job_applications")
    public suspend fun deleteAll(): Unit
}
