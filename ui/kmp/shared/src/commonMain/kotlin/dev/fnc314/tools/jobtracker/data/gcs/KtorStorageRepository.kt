package dev.fnc314.tools.jobtracker.data.gcs

import dev.fnc314.tools.jobtracker.domain.repository.StorageRepository
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*

internal class KtorStorageRepository(
    private val httpClient: HttpClient,
) : StorageRepository {

    override suspend fun getSignedUrl(path: String): String {
        // TODO: Implement GCS signed URL generation
        throw NotImplementedError("GCS signed URL generation not yet implemented")
    }

    override suspend fun downloadText(path: String): String {
        // TODO: Implement text download from GCS
        throw NotImplementedError("GCS text download not yet implemented")
    }

    override suspend fun uploadFile(path: String, data: ByteArray): String {
        // TODO: Implement file upload to GCS
        throw NotImplementedError("GCS file upload not yet implemented")
    }
}
