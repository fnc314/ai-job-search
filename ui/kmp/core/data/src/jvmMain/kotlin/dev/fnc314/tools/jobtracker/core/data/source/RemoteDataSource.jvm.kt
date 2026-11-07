package dev.fnc314.tools.jobtracker.core.data.source

import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class JvmRemoteDataSource(
    private val firebase: FirebaseApp = FirebaseApp.getInstance(),
) : RemoteDataSource {
    private val firestore: Firestore = FirestoreClient.getFirestore(firebase)

    override fun observeCollection(collectionPath: String): Flow<List<RemoteRecord>> = flow {
        val snapshot = firestore.collection(collectionPath).get().get()
        val records = snapshot.documents.map { doc ->
            MapRemoteRecord(id = doc.id, fields = doc.data ?: emptyMap())
        }
        emit(records)
    }

    override suspend fun getDocument(collectionPath: String, documentId: String): RemoteRecord? {
        val doc = firestore.collection(collectionPath).document(documentId).get().get()
        if (!doc.exists()) return null
        return MapRemoteRecord(id = doc.id, fields = doc.data ?: emptyMap())
    }

    override suspend fun setDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).set(record.fields).get()
            DataResult.Success
        } catch (e: Throwable) {
            DataResult.Failure(e)
        }
    }

    override suspend fun updateDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).update(record.fields).get()
            DataResult.Success
        } catch (e: Throwable) {
            DataResult.Failure(e)
        }
    }
}

public actual fun createRemoteDataSource(): RemoteDataSource = JvmRemoteDataSource()
