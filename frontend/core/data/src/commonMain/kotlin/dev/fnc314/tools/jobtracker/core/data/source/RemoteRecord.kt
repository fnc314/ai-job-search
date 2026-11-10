package dev.fnc314.tools.jobtracker.core.data.source

import kotlin.time.Instant

/**
 * Universal document record abstraction bridging Firestore documents and REST payloads.
 *
 * Exposes type-safe property retrieval for strings, longs, and booleans from arbitrary document representations.
 */
public interface RemoteRecord {

  /** Unique document identifier. */
  public val id: String

  /** Raw dictionary of document field mappings. */
  public val fields: Map<String, Any?>

  /**
   * Retrieves a string value associated with [key], or null if missing.
   *
   * @param key The field name.
   * @return The string value or null.
   */
  public fun getString(key: String): String?

  /**
   * Retrieves a 64-bit integer numeric value associated with [key], or null if missing.
   *
   * @param key The field name.
   * @return The long value or null.
   */
  public fun getLong(key: String): Long?

  /**
   * Retrieves a boolean value associated with [key], or null if missing.
   *
   * @param key The field name.
   * @return The boolean value or null.
   */
  public fun getBoolean(key: String): Boolean?

  /**
   * Attempts to parse [key] into an [Instant]
   * @param key The field name.
   * @return The parsed [Instant] or [Instant.DISTANT_PAST]
   */
  public fun getInstant(key: String): Instant
}
