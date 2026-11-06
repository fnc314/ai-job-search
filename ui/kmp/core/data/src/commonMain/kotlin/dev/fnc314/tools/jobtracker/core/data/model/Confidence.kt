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

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

    /**
     * Closed vocabulary of known confidence levels.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : Confidence {
        /** Fallback when no confidence calculation was performed. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "CONFIDENCE_UNSPECIFIED", wireTag = 0),

        /** High confidence in extraction accuracy (>90%). */
        HIGH_CONFIDENCE(displayName = "High", wireKey = "HIGH_CONFIDENCE", wireTag = 1),

        /** Medium confidence in extraction accuracy (60% to 90%). */
        MEDIUM_CONFIDENCE(displayName = "Medium", wireKey = "MEDIUM_CONFIDENCE", wireTag = 2),

        /** Low confidence requiring manual human verification (<60%). */
        LOW_CONFIDENCE(displayName = "Low", wireKey = "LOW_CONFIDENCE", wireTag = 3);
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
            listOf(
                entry.name.lowercase(),
                entry.name.removeSuffix("_CONFIDENCE").lowercase(),
                "confidence:${entry.name.removeSuffix("_CONFIDENCE").lowercase()}"
            )
        }

        public val UNSPECIFIED: Confidence = Known.UNSPECIFIED
        public val HIGH_CONFIDENCE: Confidence = Known.HIGH_CONFIDENCE
        public val MEDIUM_CONFIDENCE: Confidence = Known.MEDIUM_CONFIDENCE
        public val LOW_CONFIDENCE: Confidence = Known.LOW_CONFIDENCE

        // Aliases for backward compatibility
        public val HIGH: Confidence = Known.HIGH_CONFIDENCE
        public val MEDIUM: Confidence = Known.MEDIUM_CONFIDENCE
        public val LOW: Confidence = Known.LOW_CONFIDENCE

        public val entries: List<Confidence> = Known.entries

        public fun fromWireKey(raw: String?): Confidence {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): Confidence = fromWireKey(raw)
    }
}
