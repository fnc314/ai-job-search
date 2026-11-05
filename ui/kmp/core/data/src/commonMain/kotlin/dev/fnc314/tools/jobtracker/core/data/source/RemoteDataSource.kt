package dev.fnc314.tools.jobtracker.core.data.source

import kotlinx.coroutines.flow.Flow

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
