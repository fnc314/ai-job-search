package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Remote work arrangement classification governing physical office attendance expectations.
 *
 * @property displayName Formatted name for UI display.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"work-mode:remote"`).
 */
public enum class WorkMode(
    public val displayName: String,
    public val firestoreValue: String,
) {
    /** Fallback when work arrangement is unstated. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "work-mode:unspecified"),

    /** Full-time physical office attendance required. */
    ONSITE(displayName = "On-site", firestoreValue = "work-mode:onsite"),

    /** Blended schedule with required in-office days and remote flexibility. */
    HYBRID(displayName = "Hybrid", firestoreValue = "work-mode:hybrid"),

    /** 100% remote position with no mandatory office presence. */
    REMOTE(displayName = "Remote", firestoreValue = "work-mode:remote");

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = firestoreValue

    public companion object {
        /**
         * Resolves a raw or namespaced work mode string into a [WorkMode] enum.
         *
         * @param raw The raw string representation.
         * @return The matched [WorkMode] or [UNSPECIFIED].
         */
        public fun fromFirestore(raw: String?): WorkMode {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true) ||
                it.firestoreValue.removePrefix("work-mode:").equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
