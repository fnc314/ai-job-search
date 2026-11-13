package dev.fnc314.tools.jobtracker.core.artifacts.factory

import dev.fnc314.tools.jobtracker.core.artifacts.model.ArtifactStorage
import dev.fnc314.tools.jobtracker.core.artifacts.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.artifacts.model.TrackerArtifactKind
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Factory object for constructing [TrackerArtifact] instances.
 * Moved from [dev.fnc314.tools.jobtracker.core.data.factory.Factory] to avoid circular dependencies.
 */
public object ArtifactFactory {

    /**
     * Constructs a new [TrackerArtifact] instance.
     *
     * @param id Unique artifact identifier.
     * @param applicationId Associated application slug.
     * @param kind Functional artifact classification [TrackerArtifactKind]. Defaults to [TrackerArtifactKind.UNSPECIFIED].
     * @param filename Document filename with extension.
     * @param contentType MIME content type.
     * @param size Size in bytes.
     * @param sha256 Content hash.
     * @param storage Cloud Storage bucket coordinates [ArtifactStorage].
     * @param localPath Local filesystem path.
     * @param content Inline text content for small text-based artifacts.
     * @param externalUrl Remote access URL.
     * @param createdAt Creation timestamp. Defaults to [Clock.System.now].
     * @return A constructed [TrackerArtifact] instance.
     */
    public fun createTrackerArtifact(
        id: String,
        applicationId: String,
        kind: TrackerArtifactKind = TrackerArtifactKind.UNSPECIFIED,
        filename: String,
        contentType: String? = null,
        size: Long? = null,
        sha256: String? = null,
        storage: ArtifactStorage? = null,
        localPath: String? = null,
        content: String? = null,
        externalUrl: String? = null,
        createdAt: Instant? = Clock.System.now(),
    ): TrackerArtifact = TrackerArtifact(
        id = id,
        applicationId = applicationId,
        kind = kind,
        filename = filename,
        contentType = contentType,
        size = size,
        sha256 = sha256,
        storage = storage,
        localPath = localPath,
        content = content,
        externalUrl = externalUrl,
        createdAt = createdAt,
    )
}
