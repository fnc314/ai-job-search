package dev.fnc314.tools.jobtracker.core.data.model

/**
 * System actor or human participant initiating an audit event or timeline mutation.
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates canonical system actors, while [Unrecognized] captures unexpected strings.
 */
public sealed interface EventActor : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

    /**
     * Closed vocabulary of known system actors.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : EventActor {
        /** Fallback when initiator identity is not recorded. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "actor:unspecified", wireTag = 0),

        /** The human candidate interacting via the TUI, Web Dashboard, or CLI. */
        USER(displayName = "User", wireKey = "actor:user", wireTag = 1),

        /** Autonomous AI agent acting on behalf of the candidate. */
        AGENT(displayName = "Agent", wireKey = "actor:agent", wireTag = 2),

        /** Automated background email synchronization job. */
        EMAIL_SYNC(displayName = "Email Sync", wireKey = "actor:gmail-sync", wireTag = 3),

        /** Batch data import or migration process. */
        IMPORT(displayName = "Import", wireKey = "actor:import", wireTag = 4),

        /** Recurring cron scheduler triggering time-based transitions. */
        SCHEDULER(displayName = "Scheduler", wireKey = "actor:scheduler", wireTag = 5);
    }

    /**
     * Associated-value carrier for unmapped, future, or custom actor strings.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : EventActor {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: EventActor = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val aliases = mutableListOf(entry.wireKey.removePrefix("actor:"))
            if (entry == Known.EMAIL_SYNC) {
                aliases.add("actor:email-sync")
                aliases.add("email-sync")
            }
            aliases
        }

        public val UNSPECIFIED: EventActor = Known.UNSPECIFIED
        public val USER: EventActor = Known.USER
        public val AGENT: EventActor = Known.AGENT
        public val EMAIL_SYNC: EventActor = Known.EMAIL_SYNC
        public val IMPORT: EventActor = Known.IMPORT
        public val SCHEDULER: EventActor = Known.SCHEDULER

        public val entries: List<EventActor> = Known.entries

        public fun fromWireKey(raw: String?): EventActor {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): EventActor = fromWireKey(raw)
    }
}
