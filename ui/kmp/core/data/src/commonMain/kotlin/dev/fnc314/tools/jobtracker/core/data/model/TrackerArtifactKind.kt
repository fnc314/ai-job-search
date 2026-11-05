package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Functional classification of an attached document or compiled asset.
 *
 * @property displayName Formatted name for UI presentation and badge rendering.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"artifact:cv"`).
 */
public enum class TrackerArtifactKind(
    public val displayName: String,
    public val firestoreValue: String,
) {
    /** Fallback when artifact classification is unspecified. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "artifact:unspecified"),

    /** Tailored LaTeX Curriculum Vitae (PDF). */
    CV(displayName = "CV", firestoreValue = "artifact:cv"),

    /** Targeted LaTeX Cover Letter (PDF). */
    COVER_LETTER(displayName = "Cover Letter", firestoreValue = "artifact:cover-letter"),

    /** Application outcome summary or feedback document (`outcome.md`). */
    OUTCOME(displayName = "Outcome", firestoreValue = "artifact:outcome"),

    /** Archived raw HTML or text job posting specification. */
    POSTING(displayName = "Posting", firestoreValue = "artifact:posting"),

    /** Technical take-home assessment brief, code archive, or submission. */
    ASSESSMENT(displayName = "Assessment", firestoreValue = "artifact:assessment");

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = firestoreValue

    public companion object {
        /**
         * Resolves a raw or namespaced string into a [TrackerArtifactKind] enum.
         *
         * @param raw The raw string representation.
         * @return The matched [TrackerArtifactKind] or [UNSPECIFIED].
         */
        public fun fromFirestore(raw: String?): TrackerArtifactKind {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
