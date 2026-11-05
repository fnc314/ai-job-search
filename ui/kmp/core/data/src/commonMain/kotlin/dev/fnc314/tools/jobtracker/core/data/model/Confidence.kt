package dev.fnc314.tools.jobtracker.core.data.model

public enum class Confidence(
    public val displayName: String,
    public val firestoreValue: String,
    public val rawValue: String,
) {
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "confidence:unspecified", rawValue = "unspecified"),
    HIGH(displayName = "High", firestoreValue = "confidence:high", rawValue = "high"),
    MEDIUM(displayName = "Medium", firestoreValue = "confidence:medium", rawValue = "medium"),
    LOW(displayName = "Low", firestoreValue = "confidence:low", rawValue = "low");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public companion object {
        public fun fromFirestore(raw: String?): Confidence {
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
