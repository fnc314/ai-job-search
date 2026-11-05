package dev.fnc314.tools.jobtracker.core.data.model

public enum class EventActor(
    public val displayName: String,
    public val firestoreValue: String,
) {
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "actor:unspecified"),
    USER(displayName = "User", firestoreValue = "actor:user"),
    AGENT(displayName = "Agent", firestoreValue = "actor:agent"),
    EMAIL_SYNC(displayName = "Email Sync", firestoreValue = "actor:email-sync"),
    IMPORT(displayName = "Import", firestoreValue = "actor:import"),
    SCHEDULER(displayName = "Scheduler", firestoreValue = "actor:scheduler");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public companion object {
        public fun fromFirestore(raw: String?): EventActor {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true) ||
                it.firestoreValue.removePrefix("actor:").equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
