package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Functional classification of an attached document or compiled asset.
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates canonical artifact kinds, while [Unrecognized] captures unexpected strings.
 */
public sealed interface TrackerArtifactKind : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

    /**
     * Closed vocabulary of known artifact kinds.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : TrackerArtifactKind {
        /** Fallback when artifact classification is unspecified. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "UNSPECIFIED", wireTag = 0),

        /** Tailored LaTeX Curriculum Vitae (PDF). */
        CV(displayName = "CV", wireKey = "CV", wireTag = 1),

        /** Targeted LaTeX Cover Letter (PDF). */
        COVER_LETTER(displayName = "Cover Letter", wireKey = "COVER_LETTER", wireTag = 2),

        /** Application outcome summary or feedback document (`outcome.md`). */
        OUTCOME(displayName = "Outcome", wireKey = "OUTCOME", wireTag = 3),

        /** Archived raw HTML or text job posting specification. */
        POSTING(displayName = "Posting", wireKey = "POSTING", wireTag = 4),

        /** Technical take-home assessment brief, code archive, or submission. */
        ASSESSMENT(displayName = "Assessment", wireKey = "ASSESSMENT", wireTag = 5);
    }

    /**
     * Associated-value carrier for unmapped, future, or custom artifact strings.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : TrackerArtifactKind {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: TrackerArtifactKind = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf("artifact:" + entry.name.lowercase().replace("_", "-"), entry.name.lowercase().replace("_", "-"))
        }

        public val UNSPECIFIED: TrackerArtifactKind = Known.UNSPECIFIED
        public val CV: TrackerArtifactKind = Known.CV
        public val COVER_LETTER: TrackerArtifactKind = Known.COVER_LETTER
        public val OUTCOME: TrackerArtifactKind = Known.OUTCOME
        public val POSTING: TrackerArtifactKind = Known.POSTING
        public val ASSESSMENT: TrackerArtifactKind = Known.ASSESSMENT

        public val entries: List<TrackerArtifactKind> = Known.entries

        public fun fromWireKey(raw: String?): TrackerArtifactKind {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): TrackerArtifactKind = fromWireKey(raw)
    }
}
