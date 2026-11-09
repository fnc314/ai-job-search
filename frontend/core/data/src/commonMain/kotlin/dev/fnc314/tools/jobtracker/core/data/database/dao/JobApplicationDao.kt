package dev.fnc314.tools.jobtracker.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import dev.fnc314.tools.jobtracker.core.data.database.entity.JobApplicationEntity
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlinx.coroutines.flow.Flow

/**
 * Room 3 Data Access Object (DAO) defining SQLite queries for cached job applications.
 *
 * Provides reactive [Flow] observation for UI lists and asynchronous suspend operations for CRUD actions.
 */
@Dao
public interface JobApplicationDao {

    /**
     * Observes the complete collection of cached applications sorted by creation timestamp descending.
     *
     * @return A cold [Flow] emitting updated lists whenever the underlying table undergoes mutation.
     */
    @Query("SELECT * FROM job_applications ORDER BY createdAt DESC")
    public fun observeAll(): Flow<List<JobApplicationEntity>>

    /**
     * Retrieves all cached applications asynchronously.
     *
     * @return List of all stored [JobApplicationEntity] records.
     */
    @Query("SELECT * FROM job_applications")
    public suspend fun getAll(): List<JobApplicationEntity>

    /**
     * Queries an application by its primary key UUIDv7.
     *
     * @param uuid The [Uuid7] primary key identifier.
     * @return The matching [JobApplicationEntity] or null if not found.
     */
    @Query("SELECT * FROM job_applications WHERE uuid = :uuid LIMIT 1")
    public suspend fun getByUuid(uuid: Uuid7): JobApplicationEntity?

    /**
     * Queries an application by its canonical slug identifier.
     *
     * @param slugId The unique slug string (e.g., `"stripe-staff-android"`).
     * @return The matching [JobApplicationEntity] or null if not found.
     */
    @Query("SELECT * FROM job_applications WHERE slugId = :slugId LIMIT 1")
    public suspend fun getBySlugId(slugId: String): JobApplicationEntity?

    /**
     * Flexible lookup querying either by [JobApplicationEntity.slugId] or [JobApplicationEntity.uuid] string representation.
     *
     * @param idOrUuidString The slug identifier or 36-character UUID string.
     * @return The matching [JobApplicationEntity] or null if not found.
     */
    @Query("SELECT * FROM job_applications WHERE slugId = :idOrUuidString OR uuid = :idOrUuidString LIMIT 1")
    public suspend fun getByIdOrUuid(idOrUuidString: String): JobApplicationEntity?

    /**
     * Inserts or replaces a collection of application entities in a single database transaction.
     *
     * @param applications The list of [JobApplicationEntity] records to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAll(applications: List<JobApplicationEntity>): Unit

    /**
     * Inserts or replaces a single application entity.
     *
     * @param application The [JobApplicationEntity] record to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(application: JobApplicationEntity): Unit

    /**
     * Deletes an application entity identified by its primary key UUIDv7.
     *
     * @param uuid The [Uuid7] primary key to remove.
     */
    @Query("DELETE FROM job_applications WHERE uuid = :uuid")
    public suspend fun deleteByUuid(uuid: Uuid7): Unit

    /**
     * Deletes an application entity identified by its slug identifier.
     *
     * @param slugId The slug identifier to remove.
     */
    @Query("DELETE FROM job_applications WHERE slugId = :slugId")
    public suspend fun deleteBySlugId(slugId: String): Unit

    /**
     * Purges all application entities from the local database cache.
     */
    @Query("DELETE FROM job_applications")
    public suspend fun deleteAll(): Unit
}
