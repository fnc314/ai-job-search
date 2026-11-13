package dev.fnc314.tools.jobtracker.core.artifacts.model

import kotlin.time.Instant

/**
 * Domain entity representing an attached document, CV, cover letter, or job posting artifact.
 *
 * @property id Unique artifact identifier.
 * @property applicationId Associated application slug or UUID identifier.
 * @property kind Classification type [TrackerArtifactKind] (e.g. CV, Cover Letter, Posting).
 * @property filename File name including extension (e.g., `"Franco_Colaizzi_CV.pdf"`).
 * @property contentType MIME content type (e.g., `"application/pdf"`).
 * @property size File size in bytes.
 * @property sha256 Cryptographic SHA-256 checksum for content integrity verification.
 * @property storage Cloud Storage reference [ArtifactStorage] specifying the bucket and object key.
 * @property localPath Local filesystem path if cached or generated locally.
 * @property content Inline text content for small text-based artifacts (Markdown, plain text).
 * @property externalUrl Public or signed URL for remote artifact access.
 * @property createdAt Timestamp when the artifact was compiled or staged.
 * @see TrackerArtifactKind
 * @see ArtifactStorage
 */
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
    public val content: String?,
    public val externalUrl: String?,
    public val createdAt: Instant?,
)

/**
 * Cloud Storage bucket coordinates for an artifact stored in Google Cloud Storage.
 *
 * @property bucket GCS bucket name (e.g., `"job-tracker-artifacts"`).
 * @property path Object path within the bucket.
 */
public data class ArtifactStorage(
    public val bucket: String,
    public val path: String,
)
