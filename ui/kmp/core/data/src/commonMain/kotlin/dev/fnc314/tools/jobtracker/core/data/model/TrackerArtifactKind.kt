package dev.fnc314.tools.jobtracker.core.data.model

public enum class TrackerArtifactKind(
    public val displayName: String,
    public val firestoreValue: String,
) {
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "artifact:unspecified"),
    CV(displayName = "CV", firestoreValue = "artifact:cv"),
    COVER_LETTER(displayName = "Cover Letter", firestoreValue = "artifact:cover-letter"),
    OUTCOME(displayName = "Outcome", firestoreValue = "artifact:outcome"),
    POSTING(displayName = "Posting", firestoreValue = "artifact:posting"),
    ASSESSMENT(displayName = "Assessment", firestoreValue = "artifact:assessment");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public companion object {
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
