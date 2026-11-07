package dev.fnc314.tools.jobtracker.core.data.source

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal class AndroidRemoteDataSource(
    private val firebase: FirebaseApp = FirebaseApp.getInstance(),
) : RemoteDataSource {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(firebase)

    override fun observeCollection(collectionPath: String): Flow<List<RemoteRecord>> = callbackFlow {
        val listener = firestore.collection(collectionPath).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val records = snapshot?.documents?.map { doc ->
                MapRemoteRecord(id = doc.id, fields = doc.data ?: emptyMap())
            } ?: emptyList()
            trySend(records)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getDocument(collectionPath: String, documentId: String): RemoteRecord? {
        val doc = firestore.collection(collectionPath).document(documentId).get().await()
        if (!doc.exists()) return null
        return MapRemoteRecord(id = doc.id, fields = doc.data ?: emptyMap())
    }

    override suspend fun setDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).set(record.fields).await()
            DataResult.Success
        } catch (e: Throwable) {
            DataResult.Failure(e)
        }
    }

    override suspend fun updateDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult {
        return try {
            firestore.collection(collectionPath).document(documentId).update(record.fields).await()
            DataResult.Success
        } catch (e: Throwable) {
            DataResult.Failure(e)
        }
    }
}

public actual fun createRemoteDataSource(): RemoteDataSource = AndroidRemoteDataSource()
