package dev.fnc314.tools.jobtracker.core.artifacts.impl

import dev.fnc314.tools.jobtracker.core.artifacts.factory.ArtifactFactory
import dev.fnc314.tools.jobtracker.core.artifacts.model.ArtifactStorage
import dev.fnc314.tools.jobtracker.core.artifacts.model.ArtifactsRepository
import dev.fnc314.tools.jobtracker.core.artifacts.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.artifacts.model.TrackerArtifactKind
import dev.fnc314.tools.jobtracker.core.data.source.MapRemoteRecord
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.core.data.source.RemoteRecord
import dev.fnc314.tools.jobtracker.core.data.util.InstantParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant

/**
 * Implementation of [ArtifactsRepository] that queries Firestore directly
 * with an in-memory cache keyed by application ID.
 */
internal class ArtifactsRepositoryImpl(
    private val remoteDataSource: RemoteDataSource,
) : ArtifactsRepository {

    private val cache = mutableMapOf<String, List<TrackerArtifact>>()

    override suspend fun getArtifacts(applicationId: String): List<TrackerArtifact> =
        withContext(Dispatchers.Default) {
            cache[applicationId]?.let { return@withContext it }
            val collectionPath = "applications/$applicationId/artifacts"
            val records = remoteDataSource.getCollection(collectionPath)
            val artifacts = records.map {
                mapRecordToTrackerArtifact(it, applicationId)
            }
            cache[applicationId] = artifacts
            artifacts
        }

    override suspend fun getArtifact(
        applicationId: String,
        artifactId: String,
    ): TrackerArtifact? = withContext(Dispatchers.Default) {
        getArtifacts(applicationId).find { it.id == artifactId }
    }

    private fun mapRecordToTrackerArtifact(
        record: RemoteRecord,
        fallbackApplicationId: String,
    ): TrackerArtifact {
        val storageMap = record.fields["storage"] as? Map<*, *>
        val storage = if (storageMap != null) {
            val bucket = storageMap["bucket"] as? String ?: ""
            val path = storageMap["path"] as? String ?: ""
            if (bucket.isNotBlank() && path.isNotBlank()) {
                ArtifactStorage(bucket = bucket, path = path)
            } else {
                null
            }
        } else {
            null
        }

        val createdAtRaw = record.fields["createdAt"] ?: record.fields["created_at"]
        val createdAt = InstantParser.parse(createdAtRaw).takeIf { it != Instant.DISTANT_PAST }

        return ArtifactFactory.createTrackerArtifact(
            id = record.getString("artifactId") ?: record.id,
            applicationId = record.getString("applicationId") ?: fallbackApplicationId,
            kind = TrackerArtifactKind.fromFirestore(record.getString("kind")),
            filename = record.getString("filename") ?: record.id,
            contentType = record.getString("contentType"),
            size = record.getLong("size"),
            sha256 = record.getString("sha256"),
            storage = storage,
            localPath = record.getString("localPath") ?: record.getString("driveUrl"),
            content = record.getString("content"),
            externalUrl = record.getString("externalUrl"),
            createdAt = createdAt,
        )
    }
}
