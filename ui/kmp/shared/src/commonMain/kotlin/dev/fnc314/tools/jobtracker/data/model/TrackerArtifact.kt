package dev.fnc314.tools.jobtracker.data.model

import kotlin.time.Instant

data class TrackerArtifact(
    val id: String,
    val applicationId: String,
    val kind: TrackerArtifactKind,
    val filename: String,
    val contentType: String?,
    val size: Long?,
    val sha256: String?,
    val storage: ArtifactStorage?,
    val localPath: String?,
    val externalUrl: String?,
    val createdAt: Instant?,
)

data class ArtifactStorage(
    val bucket: String,
    val path: String,
)
