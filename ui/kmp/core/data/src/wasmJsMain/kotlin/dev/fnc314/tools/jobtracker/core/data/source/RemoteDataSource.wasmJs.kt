package dev.fnc314.tools.jobtracker.core.data.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class WasmJsRemoteDataSource : RemoteDataSource {
    override fun observeCollection(collectionPath: String): Flow<List<RemoteRecord>> = flowOf(emptyList())
    override suspend fun getCollection(collectionPath: String): List<RemoteRecord> = emptyList()
    override suspend fun getDocument(collectionPath: String, documentId: String): RemoteRecord? = null
    override suspend fun setDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult = DataResult.Success
    override suspend fun updateDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult = DataResult.Success
}

public actual fun createRemoteDataSource(): RemoteDataSource = WasmJsRemoteDataSource()
