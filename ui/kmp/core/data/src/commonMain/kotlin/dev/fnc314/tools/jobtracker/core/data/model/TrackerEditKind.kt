package dev.fnc314.tools.jobtracker.core.data.model

public enum class TrackerEditKind(
    public val displayName: String,
    public val firestoreValue: String,
) {
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "edit:unspecified"),
    STATUS(displayName = "Status", firestoreValue = "edit:status"),
    EVENT(displayName = "Event", firestoreValue = "edit:event"),
    SCORE(displayName = "Score", firestoreValue = "edit:score"),
    ARTIFACT(displayName = "Artifact", firestoreValue = "edit:artifact"),
    NOTE(displayName = "Note", firestoreValue = "edit:note");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public companion object {
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
