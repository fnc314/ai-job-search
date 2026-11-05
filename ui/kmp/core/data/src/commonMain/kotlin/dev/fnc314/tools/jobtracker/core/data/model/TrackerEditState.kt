package dev.fnc314.tools.jobtracker.core.data.model

public enum class TrackerEditState(
    public val displayName: String,
    public val firestoreValue: String,
) {
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "proposal:unspecified"),
    PENDING(displayName = "Pending", firestoreValue = "proposal:pending"),
    APPROVED(displayName = "Approved", firestoreValue = "proposal:approved"),
    REJECTED(displayName = "Rejected", firestoreValue = "proposal:rejected"),
    EXPIRED(displayName = "Expired", firestoreValue = "proposal:expired");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public companion object {
        public fun fromFirestore(raw: String?): TrackerEditState {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
