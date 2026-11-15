package dev.fnc314.tools.jobsearch.parser

import kotlinx.serialization.Serializable
import java.io.File
import java.security.MessageDigest

/**
 * Metadata for a file artifact (hash, size, content type).
 *
 * @property filename Base filename of the artifact (e.g. `resume.pdf`).
 * @property contentType MIME content type string.
 * @property size File size in bytes.
 * @property sha256 Hex-encoded SHA-256 digest of file contents.
 */
@Serializable
public data class ArtifactMetadata(
  /** Base filename of the artifact (e.g. `resume.pdf`). */
  public val filename: String,
  /** MIME content type string. */
  public val contentType: String,
  /** File size in bytes. */
  public val size: Long,
  /** Hex-encoded SHA-256 digest of file contents. */
  public val sha256: String,
)

/**
 * Storage location for an artifact blob.
 *
 * @property bucket Cloud storage bucket identifier.
 * @property path Path within bucket leading to the blob artifact.
 */
@Serializable
public data class ArtifactStorage(
  /** Cloud storage bucket identifier. */
  public val bucket: String,
  /** Path within bucket leading to the blob artifact. */
  public val path: String,
)

/**
 * Exception thrown when artifact operations fail.
 */
public class ArtifactError(
  message: String,
) : RuntimeException(message)

/**
 * Artifact metadata utilities mirroring Python `artifacts.py`.
 *
 * Handles file hashing, content type detection, and GCS path construction.
 */
public object ArtifactUtils {
  private const val CHUNK_SIZE: Int = 1024 * 1024

  /**
   * Valid artifact kinds per the tracker schema.
   */
  public val VALID_ARTIFACT_KINDS: Set<String> =
    setOf(
      "CV",
      "COVER_LETTER",
      "PORTFOLIO",
      "OTHER",
    )

  /**
   * Compute SHA-256 hash of a file.
   */
  public fun sha256File(path: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    path.inputStream().use { stream ->
      val buffer = ByteArray(CHUNK_SIZE)
      var bytesRead: Int
      while (stream.read(buffer).also { bytesRead = it } != -1) {
        digest.update(buffer, 0, bytesRead)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  /**
   * Guess the MIME content type for a file based on its extension.
   */
  public fun contentTypeFor(path: File): String {
    val name = path.name
    val extension = name.substringAfterLast('.', "")
    return when (extension.lowercase()) {
      "pdf" -> "application/pdf"
      "txt", "text" -> "text/plain"
      "json" -> "application/json"
      "xml" -> "application/xml"
      "html", "htm" -> "text/html"
      "csv" -> "text/csv"
      "md" -> "text/markdown"
      "png" -> "image/png"
      "jpg", "jpeg" -> "image/jpeg"
      "gif" -> "image/gif"
      "svg" -> "image/svg+xml"
      "doc" -> "application/msword"
      "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      else -> "application/octet-stream"
    }
  }

  /**
   * Describe a file: hash, size, content type. Read-only operation.
   */
  public fun describe(path: File): ArtifactMetadata {
    if (!path.exists() || !path.isFile) {
      throw ArtifactError("not a regular file: ${path.absolutePath}")
    }
    return ArtifactMetadata(
      filename = path.name,
      contentType = contentTypeFor(path),
      size = path.length(),
      sha256 = sha256File(path),
    )
  }

  /**
   * Build a canonical GCS path: `applications/{company}/{role}/{date}/{filename}`.
   *
   * Company and role names are normalized for consistent, human-readable directory structure.
   */
  public fun blobPath(
    company: String,
    role: String,
    date: String,
    kind: String,
    filename: String,
  ): String {
    if (kind !in VALID_ARTIFACT_KINDS) {
      throw ArtifactError("unknown artifact kind '$kind'")
    }
    val normCompany = DocumentIdGenerator.normalizeDirName(company)
    val normRole = DocumentIdGenerator.normalizeDirName(role)
    return "applications/$normCompany/$normRole/$date/$filename"
  }
}