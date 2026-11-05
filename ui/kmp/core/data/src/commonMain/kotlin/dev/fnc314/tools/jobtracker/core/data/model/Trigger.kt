package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Trigger source responsible for initiating an automated workflow or edit proposal.
 *
 * @property displayName Formatted name for UI display.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"trigger:manual"`).
 */
public enum class Trigger(
    public val displayName: String,
    public val firestoreValue: String,
) {
    /** Fallback when trigger source is unknown. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "trigger:unspecified"),

    /** Explicit human action via UI or CLI. */
    MANUAL(displayName = "Manual", firestoreValue = "trigger:manual"),

    /** Ingestion triggered by incoming Gmail message receipt. */
    EMAIL_SYNC(displayName = "Email Sync", firestoreValue = "trigger:email-sync"),

    /** Scheduled cron or timer job. */
    SCHEDULER(displayName = "Scheduler", firestoreValue = "trigger:scheduler"),

    /** Triggered by batch import process. */
    IMPORT(displayName = "Import", firestoreValue = "trigger:import");

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = firestoreValue

    public companion object {
        /**
         * Resolves a raw or namespaced trigger string into a [Trigger] enum.
         *
         * @param raw The raw string representation.
         * @return The matched [Trigger] or [UNSPECIFIED].
         */
        public fun fromFirestore(raw: String?): Trigger {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true) ||
                it.firestoreValue.removePrefix("trigger:").equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
