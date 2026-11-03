package dev.fnc314.tools.jobtracker.domain.repository

interface StorageRepository {
    suspend fun getSignedUrl(path: String): String
    suspend fun downloadText(path: String): String
    suspend fun uploadFile(path: String, data: ByteArray): String
}
