package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Machine extraction confidence level assigned by automated parsers or LLM evaluators.
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates canonical confidence levels, while [Unrecognized] captures unexpected strings.
 */
public sealed interface Confidence : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the namespaced Firestore document string. */
    public fun toFirestore(): String = wireKey

    /**
     * Closed vocabulary of known confidence levels.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        public val rawValue: String,
        override val wireTag: Int = -1,
    ) : Confidence {
        /** Fallback when no confidence calculation was performed. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "confidence:unspecified", rawValue = "unspecified", wireTag = 0),

        /** High confidence in extraction accuracy (>90%). */
        HIGH(displayName = "High", wireKey = "confidence:high", rawValue = "high", wireTag = 1),

        /** Medium confidence in extraction accuracy (60% to 90%). */
        MEDIUM(displayName = "Medium", wireKey = "confidence:medium", rawValue = "medium", wireTag = 2),

        /** Low confidence requiring manual human verification (<60%). */
        LOW(displayName = "Low", wireKey = "confidence:low", rawValue = "low", wireTag = 3);
    }

    /**
     * Associated-value carrier for unmapped, future, or custom confidence strings.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : Confidence {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: Confidence = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf(entry.rawValue)
        }

        public val UNSPECIFIED: Confidence = Known.UNSPECIFIED
        public val HIGH: Confidence = Known.HIGH
        public val MEDIUM: Confidence = Known.MEDIUM
        public val LOW: Confidence = Known.LOW

        public val entries: List<Confidence> = Known.entries

        public fun fromWireKey(raw: String?): Confidence {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): Confidence = fromWireKey(raw)
    }
}
