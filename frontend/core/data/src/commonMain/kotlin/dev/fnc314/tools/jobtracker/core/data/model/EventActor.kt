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
        UNSPECIFIED(displayName = "Unspecified", wireKey = "UNSPECIFIED", wireTag = 0),

        /** The human candidate interacting via the TUI, Web Dashboard, or CLI. */
        USER(displayName = "User", wireKey = "USER", wireTag = 1),

        /** Autonomous AI agent acting on behalf of the candidate. */
        AGENT(displayName = "Agent", wireKey = "AGENT", wireTag = 2),

        /** Automated background email synchronization job. */
        EMAIL_SYNC(displayName = "Email Sync", wireKey = "EMAIL_SYNC", wireTag = 3),

        /** Batch data import or migration process. */
        IMPORT(displayName = "Import", wireKey = "IMPORT", wireTag = 4),

        /** Recurring cron scheduler triggering time-based transitions. */
        SCHEDULER(displayName = "Scheduler", wireKey = "SCHEDULER", wireTag = 5);
    }

    /**
     * Associated-value carrier for unmapped, future, or custom actor strings.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : EventActor {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    /** Companion object providing lookup and shortcut accessors for [EventActor]. */
    public companion object {
        /** Default fallback value for [EventActor]. */
        public val DEFAULT: EventActor = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val aliases = mutableListOf(
                "actor:" + entry.name.lowercase().replace("_", "-"),
                entry.name.lowercase().replace("_", "-")
            )
            if (entry == Known.EMAIL_SYNC) {
                aliases.add("actor:gmail-sync")
                aliases.add("actor:email-sync")
                aliases.add("gmail-sync")
                aliases.add("email-sync")
            }
            aliases
        }

        /** Fallback when initiator identity is not recorded. */
        public val UNSPECIFIED: EventActor = Known.UNSPECIFIED
        /** The human candidate interacting via the TUI, Web Dashboard, or CLI. */
        public val USER: EventActor = Known.USER
        /** Autonomous AI agent acting on behalf of the candidate. */
        public val AGENT: EventActor = Known.AGENT
        /** Automated background email synchronization job. */
        public val EMAIL_SYNC: EventActor = Known.EMAIL_SYNC
        /** Batch data import or migration process. */
        public val IMPORT: EventActor = Known.IMPORT
        /** Recurring cron scheduler triggering time-based transitions. */
        public val SCHEDULER: EventActor = Known.SCHEDULER

        /** All canonical [EventActor] entries. */
        public val entries: List<EventActor> = Known.entries

        /**
         * Resolves a [EventActor] instance from its raw wire key string.
         *
         * @param raw Raw wire key string from external storage or serialized payload.
         * @return Matching canonical entry, or an unrecognized fallback instance.
         */
        public fun fromWireKey(raw: String?): EventActor {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        /** Parses a wire representation into [EventActor]. */
        public fun fromFirestore(raw: String?): EventActor = fromWireKey(raw)
    }
}
