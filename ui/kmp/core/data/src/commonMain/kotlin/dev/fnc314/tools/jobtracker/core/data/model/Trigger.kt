package dev.fnc314.tools.jobtracker.core.data.model

public enum class Trigger(
    public val displayName: String,
    public val firestoreValue: String,
) {
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "trigger:unspecified"),
    MANUAL(displayName = "Manual", firestoreValue = "trigger:manual"),
    EMAIL_SYNC(displayName = "Email Sync", firestoreValue = "trigger:email-sync"),
    SCHEDULER(displayName = "Scheduler", firestoreValue = "trigger:scheduler"),
    IMPORT(displayName = "Import", firestoreValue = "trigger:import");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public companion object {
        public fun fromFirestore(raw: String?): Trigger {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
