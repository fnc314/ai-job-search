package dev.fnc314.tools.jobtracker.core.data.source

import dev.fnc314.tools.jobtracker.core.data.util.InstantParser
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Multiplatform contract defining remote database interactions with Firestore or REST endpoints.
 */
public interface RemoteDataSource {

  /**
   * Observes real-time document additions, updates, and removals in the specified collection path.
   *
   * @param collectionPath Target collection path (e.g. `"applications"`).
   * @return Cold [Flow] emitting the updated list of [RemoteRecord] documents.
   */
  public fun observeCollection(collectionPath: String): Flow<List<RemoteRecord>>

  /**
   * Retrieves all documents from the specified remote collection path.
   *
   * @param collectionPath Target collection path (e.g. `"applications"`).
   * @return List of current [RemoteRecord] documents.
   */
  public suspend fun getCollection(collectionPath: String): List<RemoteRecord>

  /**
   * Retrieves a single remote document by path and identifier.
   *
   * @param collectionPath Parent collection path.
   * @param documentId Document identifier key.
   * @return The fetched [RemoteRecord] or null if not found.
   */
  public suspend fun getDocument(collectionPath: String, documentId: String): RemoteRecord?

  /**
   * Writes or overwrites a remote document.
   *
   * @param collectionPath Target collection path.
   * @param documentId Document key.
   * @param record The [RemoteRecord] content to persist.
   * @return [DataResult.Success] on completion or [DataResult.Failure] on error.
   */
  public suspend fun setDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult

  /**
   * Partially updates existing fields in a remote document.
   *
   * @param collectionPath Target collection path.
   * @param documentId Document key.
   * @param record The [RemoteRecord] containing modified fields.
   * @return [DataResult.Success] on completion or [DataResult.Failure] on error.
   */
  public suspend fun updateDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult
}

/**
 * Data class implementation of [RemoteRecord] wrapping a map of document fields.
 */
public data class MapRemoteRecord(
  override val id: String,
  override val fields: Map<String, Any?>,
) : RemoteRecord {

  override fun getString(key: String): String? = fields[key] as? String
  override fun getLong(key: String): Long? = (fields[key] as? Number)?.toLong()
  override fun getBoolean(key: String): Boolean? = fields[key] as? Boolean
  override fun getInstant(key: String): Instant = InstantParser.parse(
    fields[key]
  )
}

/**
 * Platform-specific factory resolving the native [RemoteDataSource] implementation wrapping Firebase.
 */
public expect fun createRemoteDataSource(): RemoteDataSource
