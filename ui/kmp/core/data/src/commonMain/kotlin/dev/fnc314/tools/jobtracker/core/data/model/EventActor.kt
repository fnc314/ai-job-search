package dev.fnc314.tools.jobtracker.core.data.model

/**
 * System actor or human participant initiating an audit event or timeline mutation.
 *
 * @property displayName Formatted name for UI display.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"actor:user"`).
 */
public enum class EventActor(
    public val displayName: String,
    public val firestoreValue: String,
) {
    /** Fallback when initiator identity is not recorded. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "actor:unspecified"),

    /** The human candidate interacting via the TUI, Web Dashboard, or CLI. */
    USER(displayName = "User", firestoreValue = "actor:user"),

    /** Autonomous AI agent acting on behalf of the candidate. */
    AGENT(displayName = "Agent", firestoreValue = "actor:agent"),

    /** Automated background email synchronization job. */
    EMAIL_SYNC(displayName = "Email Sync", firestoreValue = "actor:email-sync"),

    /** Batch data import or migration process. */
    IMPORT(displayName = "Import", firestoreValue = "actor:import"),

    /** Recurring cron scheduler triggering time-based transitions. */
    SCHEDULER(displayName = "Scheduler", firestoreValue = "actor:scheduler");

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = firestoreValue

    public companion object {
        /**
         * Resolves a raw or namespaced actor string into an [EventActor] enum.
         *
         * @param raw The raw string representation.
         * @return The matched [EventActor] or [UNSPECIFIED].
         */
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
