package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Originating source mechanism from which an event, timestamp, or status transition was derived.
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates canonical provenance types, while [Unrecognized] captures unexpected strings.
 */
public sealed interface DerivedFrom : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the raw Firestore storage string. */
    public fun toFirestore(): String = wireKey

    /**
     * Closed vocabulary of known provenance mechanisms.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        public val legacyValue: String = "",
        override val wireTag: Int = -1,
    ) : DerivedFrom {
        /** Fallback when source provenance is unknown. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "derived:unspecified", legacyValue = "unspecified", wireTag = 0),

        /** Explicitly entered by the user via UI or CLI command. */
        MANUAL(displayName = "Manual", wireKey = "manual", legacyValue = "derived:manual", wireTag = 1),

        /** Inferred from freeform markdown notes via regular expressions. */
        NOTES_REGEX(displayName = "Notes Regex", wireKey = "notes-regex", legacyValue = "derived:notes-regex", wireTag = 2),

        /** Derived from automated Gmail message synchronization. */
        EMAIL(displayName = "Email", wireKey = "gmail", legacyValue = "derived:email", wireTag = 3),

        /** Imported from historical CSV dataset archives. */
        CSV_IMPORT(displayName = "CSV Import", wireKey = "csv-import", legacyValue = "derived:csv-import", wireTag = 4);
    }

    /**
     * Associated-value carrier for unmapped, future, or custom derivation strings.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : DerivedFrom {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: DerivedFrom = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val aliases = mutableListOf<String>()
            if (entry.legacyValue.isNotEmpty()) aliases.add(entry.legacyValue)
            if (entry.wireKey == "gmail") aliases.add("email")
            aliases
        }

        public val UNSPECIFIED: DerivedFrom = Known.UNSPECIFIED
        public val MANUAL: DerivedFrom = Known.MANUAL
        public val NOTES_REGEX: DerivedFrom = Known.NOTES_REGEX
        public val EMAIL: DerivedFrom = Known.EMAIL
        public val CSV_IMPORT: DerivedFrom = Known.CSV_IMPORT

        public val entries: List<DerivedFrom> = Known.entries

        public fun fromWireKey(raw: String?): DerivedFrom {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): DerivedFrom = fromWireKey(raw)
    }
}
