package dev.fnc314.tools.jobsearch.parser

/**
 * Classification of interview round format and structure.
 */
public enum class InterviewProgressType {
  UNSPECIFIED,
  RECRUITER_SCREEN,
  TECHNICAL_KNOWLEDGE,
  TECHNICAL_DEMONSTRATION,
  SYSTEMS_DESIGN,
  BEHAVIORAL,
  ON_SITE,
  FINAL_ROUND,
  ;

  /**
   * Formats the enum entry into a human-readable title.
   *
   * @return Formatted stage title (e.g. "Recruiter Screen", "Technical Knowledge").
   */
  public fun displayName(): String =
    when (this) {
      UNSPECIFIED -> "Interview"
      RECRUITER_SCREEN -> "Recruiter Screen"
      TECHNICAL_KNOWLEDGE -> "Technical Knowledge"
      TECHNICAL_DEMONSTRATION -> "Technical Demonstration"
      SYSTEMS_DESIGN -> "Systems Design"
      BEHAVIORAL -> "Behavioral"
      ON_SITE -> "On-Site Loop"
      FINAL_ROUND -> "Final Round"
    }

  /**
   * Factory methods and parsers for [InterviewProgressType].
   */
  public companion object {
    /**
     * Resolves a raw string into an [InterviewProgressType], defaulting to [UNSPECIFIED].
     *
     * @param raw The raw type identifier string.
     * @return Matching [InterviewProgressType] or [UNSPECIFIED].
     */
    public fun fromString(raw: String?): InterviewProgressType {
      if (raw.isNullOrBlank()) return UNSPECIFIED
      val normalized = raw.trim().uppercase().replace("-", "_")
      return entries.find { it.name == normalized } ?: UNSPECIFIED
    }
  }
}

/**
 * Flexible progress record tracking dynamic multi-round interview loops.
 *
 * Encapsulates the three-timestamp temporal contract:
 * - [interviewTimestamp]: The actual point-in-time when the interview session occurs.
 * - [scheduledAt]: When the interview slot was confirmed or locked in.
 * - [completedAt]: When the interview concluded and feedback was captured.
 *
 * @property currentRound Index of current round (1-based).
 * @property totalRounds Total planned rounds in loop, or 0 if unconstrained.
 * @property type Classified interview stage structure.
 * @property title Custom descriptive title for non-standard rounds.
 * @property interviewTimestamp ISO-8601 string when session occurs.
 * @property scheduledAt ISO-8601 string when slot was scheduled.
 * @property completedAt ISO-8601 string when round concluded.
 * @property notes Candidate notes or feedback regarding round.
 */
public data class InterviewProgress(
  /** Index of current round (1-based). */
  public val currentRound: Int,
  /** Total planned rounds in loop, or 0 if unconstrained. */
  public val totalRounds: Int = 0,
  /** Classified interview stage structure. */
  public val type: InterviewProgressType = InterviewProgressType.UNSPECIFIED,
  /** Custom descriptive title for non-standard rounds. */
  public val title: String? = null,
  /** ISO-8601 string when session occurs. */
  public val interviewTimestamp: String? = null,
  /** ISO-8601 string when slot was scheduled. */
  public val scheduledAt: String? = null,
  /** ISO-8601 string when round concluded. */
  public val completedAt: String? = null,
  /** Candidate notes or feedback regarding round. */
  public val notes: String? = null,
) {
  /**
   * Concise summary string for display, e.g. "Round 1/3 (Technical Knowledge)"
   */
  public fun formattedSummary(): String =
    buildString {
      append("Round $currentRound")
      if (totalRounds > 0) {
        append("/$totalRounds")
      }
      if (type != InterviewProgressType.UNSPECIFIED) {
        append(" (${type.displayName()})")
      } else if (!title.isNullOrBlank()) {
        append(" ($title)")
      }
    }

  /**
   * Compact status badge string, e.g. "1/3" or "1"
   */
  public fun roundBadge(): String = if (totalRounds > 0) "$currentRound/$totalRounds" else "$currentRound"

  /**
   * Serializes this interview progress state into a map structure suitable for Firestore.
   *
   * @return Map representation of the interview progress record.
   */
  public fun toMap(): Map<String, Any?> =
    buildMap {
      put("currentRound", currentRound)
      if (totalRounds > 0) put("totalRounds", totalRounds)
      if (type != InterviewProgressType.UNSPECIFIED) put("type", type.name)
      title?.let { put("title", it) }
      interviewTimestamp?.let { put("interviewTimestamp", it) }
      scheduledAt?.let { put("scheduledAt", it) }
      completedAt?.let { put("completedAt", it) }
      notes?.let { put("notes", it) }
    }

  /**
   * Factory methods and deserializers for [InterviewProgress].
   */
  public companion object {
    /**
     * Reconstructs an [InterviewProgress] instance from a Firestore document map.
     *
     * @param map Raw map data from remote document or local cache.
     * @return Parsed [InterviewProgress] or null if invalid or missing round index.
     */
    public fun fromMap(map: Map<String, Any?>?): InterviewProgress? {
      if (map == null) return null
      val round =
        (map["currentRound"] as? Number)?.toInt()
          ?: (map["current_round"] as? Number)?.toInt()
          ?: return null
      val total =
        (map["totalRounds"] as? Number)?.toInt()
          ?: (map["total_rounds"] as? Number)?.toInt()
          ?: 0
      val typeStr = (map["type"] as? String)
      val title = (map["title"] as? String)
      val ts = (map["interviewTimestamp"] as? String) ?: (map["interview_timestamp"] as? String)
      val sched = (map["scheduledAt"] as? String) ?: (map["scheduled_at"] as? String)
      val comp = (map["completedAt"] as? String) ?: (map["completed_at"] as? String)
      val notes = (map["notes"] as? String)
      return InterviewProgress(
        currentRound = round,
        totalRounds = total,
        type = InterviewProgressType.fromString(typeStr),
        title = title,
        interviewTimestamp = ts,
        scheduledAt = sched,
        completedAt = comp,
        notes = notes,
      )
    }
  }
}