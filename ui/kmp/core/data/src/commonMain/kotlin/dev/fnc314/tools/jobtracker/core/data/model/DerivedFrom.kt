package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Originating source mechanism from which an event, timestamp, or status transition was derived.
 *
 * @property displayName Human-readable label for UI display.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"derived:manual"`).
 * @property rawValue Legacy document string (e.g., `"gmail"` or `"manual"`).
 */
public enum class DerivedFrom(
    public val displayName: String,
    public val firestoreValue: String,
    public val rawValue: String,
) {
    /** Fallback when source provenance is unknown. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "derived:unspecified", rawValue = "unspecified"),

    /** Explicitly entered by the user via UI or CLI command. */
    MANUAL(displayName = "Manual", firestoreValue = "derived:manual", rawValue = "manual"),

    /** Inferred from freeform markdown notes via regular expressions. */
    NOTES_REGEX(displayName = "Notes Regex", firestoreValue = "derived:notes-regex", rawValue = "notes-regex"),

    /** Derived from automated Gmail message synchronization. */
    EMAIL(displayName = "Email", firestoreValue = "derived:email", rawValue = "gmail"),

    /** Imported from historical CSV dataset archives. */
    CSV_IMPORT(displayName = "CSV Import", firestoreValue = "derived:csv-import", rawValue = "csv-import");

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the raw Firestore storage string. */
    public fun toFirestore(): String = rawValue

    public companion object {
        /**
         * Resolves a raw or namespaced derivation string into a [DerivedFrom] enum.
         *
         * @param raw The raw string representation.
         * @return The matched [DerivedFrom] or [UNSPECIFIED].
         */
        public fun fromFirestore(raw: String?): DerivedFrom {
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
