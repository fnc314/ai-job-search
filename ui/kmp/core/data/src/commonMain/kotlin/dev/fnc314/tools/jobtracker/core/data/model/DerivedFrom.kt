package dev.fnc314.tools.jobtracker.core.data.model

public enum class DerivedFrom(
    public val displayName: String,
    public val firestoreValue: String,
    public val rawValue: String,
) {
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "derived:unspecified", rawValue = "unspecified"),
    MANUAL(displayName = "Manual", firestoreValue = "derived:manual", rawValue = "manual"),
    NOTES_REGEX(displayName = "Notes Regex", firestoreValue = "derived:notes-regex", rawValue = "notes-regex"),
    EMAIL(displayName = "Email", firestoreValue = "derived:email", rawValue = "gmail"),
    CSV_IMPORT(displayName = "CSV Import", firestoreValue = "derived:csv-import", rawValue = "csv-import");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = rawValue

    public companion object {
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
