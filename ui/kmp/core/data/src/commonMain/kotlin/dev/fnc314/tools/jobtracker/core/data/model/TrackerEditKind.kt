package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Categorical scope of a proposed modification within a [TrackerEdit].
 *
 * @property displayName Formatted name for UI display.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"edit:status"`).
 */
public enum class TrackerEditKind(
    public val displayName: String,
    public val firestoreValue: String,
) {
    /** Fallback when edit target is unspecified. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "edit:unspecified"),

    /** Status transition update (e.g. from Applied to Interview). */
    STATUS(displayName = "Status", firestoreValue = "edit:status"),

    /** Addition of an audit event to the application timeline. */
    EVENT(displayName = "Event", firestoreValue = "edit:event"),

    /** Recalculation or update of the candidate-to-job fit score. */
    SCORE(displayName = "Score", firestoreValue = "edit:score"),

    /** Attachment or update of a document artifact reference. */
    ARTIFACT(displayName = "Artifact", firestoreValue = "edit:artifact"),

    /** Addition or amendment of application strategy notes. */
    NOTE(displayName = "Note", firestoreValue = "edit:note");

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = firestoreValue

    public companion object {
        /**
         * Resolves a raw or namespaced string into a [TrackerEditKind] enum.
         *
         * @param raw The raw string representation.
         * @return The matched [TrackerEditKind] or [UNSPECIFIED].
         */
        public fun fromFirestore(raw: String?): TrackerEditKind {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
