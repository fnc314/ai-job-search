package dev.fnc314.tools.jobtracker.core.data.source

import kotlinx.coroutines.flow.Flow

public interface RemoteDataSource {
    public fun observeCollection(collectionPath: String): Flow<List<RemoteRecord>>
    public suspend fun getDocument(collectionPath: String, documentId: String): RemoteRecord?
    public suspend fun setDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult
    public suspend fun updateDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult
}
