package dev.fnc314.aijobtracker.frontend.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import dev.fnc314.aijobtracker.frontend.core.data.database.entity.ApplyMessageEntity
import dev.fnc314.aijobtracker.frontend.core.data.database.entity.ApplySessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object managing local persistence and reactive streams for Apply Assistant sessions
 * and timeline messages.
 */
@Dao
public interface ApplyAssistantDao {
    /**
     * Observes the most recently updated [ApplySessionEntity], or null if no session exists.
     *
     * @return Reactive [Flow] emitting the latest session.
     */
    @Query("SELECT * FROM apply_sessions ORDER BY updatedAt DESC LIMIT 1")
    public fun observeLatestSession(): Flow<ApplySessionEntity?>

    /**
     * Fetches the most recently updated [ApplySessionEntity] synchronously within a coroutine.
     *
     * @return Latest [ApplySessionEntity], or null if none exists.
     */
    @Query("SELECT * FROM apply_sessions ORDER BY updatedAt DESC LIMIT 1")
    public suspend fun getLatestSession(): ApplySessionEntity?

    /**
     * Fetches an [ApplySessionEntity] by its unique [sessionId].
     *
     * @param sessionId Target session identifier.
     * @return Matching [ApplySessionEntity] or null.
     */
    @Query("SELECT * FROM apply_sessions WHERE id = :sessionId LIMIT 1")
    public suspend fun getSession(sessionId: String): ApplySessionEntity?

    /**
     * Observes all [ApplyMessageEntity] items associated with [sessionId] ordered chronologically.
     *
     * @param sessionId Target session identifier.
     * @return Reactive [Flow] of messages.
     */
    @Query("SELECT * FROM apply_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    public fun observeMessages(sessionId: String): Flow<List<ApplyMessageEntity>>

    /**
     * Inserts or replaces an [ApplySessionEntity].
     *
     * @param session Session snapshot to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun upsertSession(session: ApplySessionEntity)

    /**
     * Inserts or replaces an [ApplyMessageEntity].
     *
     * @param message Message entity to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun upsertMessage(message: ApplyMessageEntity)

    /**
     * Deletes all persisted sessions.
     */
    @Query("DELETE FROM apply_sessions")
    public suspend fun clearSessions()

    /**
     * Deletes all persisted messages.
     */
    @Query("DELETE FROM apply_messages")
    public suspend fun clearMessages()
}
