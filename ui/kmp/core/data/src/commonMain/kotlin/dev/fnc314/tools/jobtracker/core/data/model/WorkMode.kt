package dev.fnc314.tools.jobtracker.core.data.model

public enum class WorkMode(
    public val displayName: String,
    public val firestoreValue: String,
) {
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "work-mode:unspecified"),
    ONSITE(displayName = "On-site", firestoreValue = "work-mode:onsite"),
    HYBRID(displayName = "Hybrid", firestoreValue = "work-mode:hybrid"),
    REMOTE(displayName = "Remote", firestoreValue = "work-mode:remote");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public companion object {
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
