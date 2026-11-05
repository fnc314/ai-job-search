package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.time.Instant

public data class TrackerArtifact(
    public val id: String,
    public val applicationId: String,
    public val kind: TrackerArtifactKind,
    public val filename: String,
    public val contentType: String?,
    public val size: Long?,
    public val sha256: String?,
    public val storage: ArtifactStorage?,
    public val localPath: String?,
    public val externalUrl: String?,
    public val createdAt: Instant?,
)

public data class ArtifactStorage(
    public val bucket: String,
    public val path: String,
)
