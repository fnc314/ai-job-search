package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Machine extraction confidence level assigned by automated parsers or LLM evaluators.
 *
 * @property displayName Human-readable label for display.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"confidence:high"`).
 * @property rawValue Legacy un-namespaced string value (e.g. `"high"`).
 */
public enum class Confidence(
    public val displayName: String,
    public val firestoreValue: String,
    public val rawValue: String,
) {
    /** Fallback when no confidence calculation was performed. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "confidence:unspecified", rawValue = "unspecified"),

    /** High confidence in extraction accuracy (>90%). */
    HIGH(displayName = "High", firestoreValue = "confidence:high", rawValue = "high"),

    /** Medium confidence in extraction accuracy (60% to 90%). */
    MEDIUM(displayName = "Medium", firestoreValue = "confidence:medium", rawValue = "medium"),

    /** Low confidence requiring manual human verification (<60%). */
    LOW(displayName = "Low", firestoreValue = "confidence:low", rawValue = "low");

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the namespaced Firestore document string. */
    public fun toFirestore(): String = firestoreValue

    public companion object {
        /**
         * Resolves a raw or namespaced confidence string into a [Confidence] enum.
         *
         * @param raw The raw string.
         * @return The matched [Confidence] or [UNSPECIFIED].
         */
        public fun fromFirestore(raw: String?): Confidence {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.rawValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
