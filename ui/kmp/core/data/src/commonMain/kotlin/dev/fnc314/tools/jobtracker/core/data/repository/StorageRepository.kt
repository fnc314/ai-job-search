package dev.fnc314.tools.jobtracker.core.data.repository

public interface StorageRepository {
    public suspend fun getSignedUrl(path: String): String
    public suspend fun downloadText(path: String): String
    public suspend fun uploadFile(path: String, data: ByteArray): String
}
