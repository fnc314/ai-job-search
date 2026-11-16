package dev.fnc314.aijobtracker.frontend.core.artifacts.model

import dev.fnc314.aijobtracker.frontend.core.data.model.WireContract
import dev.fnc314.aijobtracker.frontend.core.data.model.toWireLookupMap

/**
 * Functional classification of an attached document or compiled asset.
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates canonical artifact kinds, while [Unrecognized] captures unexpected strings.
 */
public sealed interface TrackerArtifactKind : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical remote storage document string representation. */
    public fun toRemote(): String = wireKey

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

    /** Companion object providing lookup and shortcut accessors for [TrackerArtifactKind]. */
    public companion object {
        /** Default fallback value for [TrackerArtifactKind]. */
        public val DEFAULT: TrackerArtifactKind = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf("artifact:" + entry.name.lowercase().replace("_", "-"), entry.name.lowercase().replace("_", "-"))
        }

        /** Fallback when artifact classification is unspecified. */
        public val UNSPECIFIED: TrackerArtifactKind = Known.UNSPECIFIED
        /** Tailored LaTeX Curriculum Vitae (PDF). */
        public val CV: TrackerArtifactKind = Known.CV
        /** Targeted LaTeX Cover Letter (PDF). */
        public val COVER_LETTER: TrackerArtifactKind = Known.COVER_LETTER
        /** Application outcome summary or feedback document (`outcome.md`). */
        public val OUTCOME: TrackerArtifactKind = Known.OUTCOME
        /** Archived raw HTML or text job posting specification. */
        public val POSTING: TrackerArtifactKind = Known.POSTING
        /** Technical take-home assessment brief, code archive, or submission. */
        public val ASSESSMENT: TrackerArtifactKind = Known.ASSESSMENT

        /** All canonical [TrackerArtifactKind] entries. */
        public val entries: List<TrackerArtifactKind> = Known.entries

        /**
         * Resolves a [TrackerArtifactKind] instance from its raw wire key string.
         *
         * @param raw Raw wire key string from external storage or serialized payload.
         * @return Matching canonical entry, or an unrecognized fallback instance.
         */
        public fun fromWireKey(raw: String?): TrackerArtifactKind {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        /** Parses a wire representation into [TrackerArtifactKind]. */
        public fun fromRemote(raw: String?): TrackerArtifactKind = fromWireKey(raw)
    }
}
