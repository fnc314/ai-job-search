package dev.fnc314.tools.jobtracker.core.data.source

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private class JvmRemoteDataSource(
    private val firebase: FirebaseApp = FirebaseApp.getInstance(),
) : RemoteDataSource {
    private val firestore: Firestore = FirestoreClient.getFirestore(firebase)

    override fun observeCollection(collectionPath: String): Flow<List<RemoteRecord>> = flow {
        val snapshot = firestore.collection(collectionPath).get().get()
        val records = snapshot.documents.map { doc ->
            MapRemoteRecord(id = doc.id, fields = sanitizeFirestoreMap(doc.data))
        }
        emit(records)
    }

    override suspend fun getDocument(collectionPath: String, documentId: String): RemoteRecord? {
        val doc = firestore.collection(collectionPath).document(documentId).get().get()
        if (!doc.exists()) return null
        return MapRemoteRecord(id = doc.id, fields = sanitizeFirestoreMap(doc.data ?: emptyMap()))
    }

    override suspend fun setDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).set(record.fields).get()
            DataResult.Success
        } catch (expected: Throwable) {
            DataResult.Failure(expected)
        }
    }

    override suspend fun updateDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).update(record.fields).get()
            DataResult.Success
        } catch (expected: Throwable) {
            DataResult.Failure(expected)
        }
    }
}

/**
 * Converts platform-specific Firestore values into multiplatform types so SDK classes never leak into common code.
 * The server SDK yields [Timestamp] for timestamp fields; read via [Timestamp.getSeconds] / [Timestamp.getNanos].
 */
private fun sanitizeFirestoreMap(map: Map<String, Any?>): Map<String, Any?> =
    map.mapValues { (_, value) -> sanitizeFirestoreValue(value) }

private fun sanitizeFirestoreValue(value: Any?): Any? = when (value) {
    is Timestamp -> Instant.fromEpochSeconds(value.seconds, value.nanos.toLong())
    is Map<*, *> -> value.entries.associate { (k, v) -> k.toString() to sanitizeFirestoreValue(v) }
    is List<*> -> value.map { sanitizeFirestoreValue(it) }
    else -> value
}

public actual fun createRemoteDataSource(): RemoteDataSource = JvmRemoteDataSource()
