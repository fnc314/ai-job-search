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
    FINAL_ROUND;

    public fun displayName(): String = when (this) {
        UNSPECIFIED -> "Interview"
        RECRUITER_SCREEN -> "Recruiter Screen"
        TECHNICAL_KNOWLEDGE -> "Technical Knowledge"
        TECHNICAL_DEMONSTRATION -> "Technical Demonstration"
        SYSTEMS_DESIGN -> "Systems Design"
        BEHAVIORAL -> "Behavioral"
        ON_SITE -> "On-Site Loop"
        FINAL_ROUND -> "Final Round"
    }

    public companion object {
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
 */
public data class InterviewProgress(
    val currentRound: Int,
    val totalRounds: Int = 0,
    val type: InterviewProgressType = InterviewProgressType.UNSPECIFIED,
    val title: String? = null,
    val interviewTimestamp: String? = null,
    val scheduledAt: String? = null,
    val completedAt: String? = null,
    val notes: String? = null,
) {
    /**
     * Concise summary string for display, e.g. "Round 1/3 (Technical Knowledge)"
     */
    public fun formattedSummary(): String = buildString {
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

    public fun toMap(): Map<String, Any?> = buildMap {
        put("currentRound", currentRound)
        if (totalRounds > 0) put("totalRounds", totalRounds)
        if (type != InterviewProgressType.UNSPECIFIED) put("type", type.name)
        title?.let { put("title", it) }
        interviewTimestamp?.let { put("interviewTimestamp", it) }
        scheduledAt?.let { put("scheduledAt", it) }
        completedAt?.let { put("completedAt", it) }
        notes?.let { put("notes", it) }
    }

    public companion object {
        public fun fromMap(map: Map<String, Any?>?): InterviewProgress? {
            if (map == null) return null
            val round = (map["currentRound"] as? Number)?.toInt()
                ?: (map["current_round"] as? Number)?.toInt()
                ?: return null
            val total = (map["totalRounds"] as? Number)?.toInt()
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
