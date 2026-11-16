package dev.fnc314.aijobtracker.frontend.core.data.model

/**
 * Trigger source responsible for initiating an automated workflow or edit proposal.
 *
 * Implemented as a sealed interface hierarchy: [Known] holds canonical triggers while
 * [Unrecognized] preserves unexpected wire strings verbatim.
 */
public sealed interface Trigger : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical remote document string representation. */
    public fun toRemote(): String = wireKey

    /** Closed vocabulary of known trigger sources. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : Trigger {
        /** Fallback when trigger source is unknown. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "UNSPECIFIED", wireTag = 0),

        /** Scheduled cron or timer job. */
        CRON(displayName = "Cron", wireKey = "CRON", wireTag = 1),

        /** Fired when an application record is created. */
        APPLICATION_CREATED(displayName = "Application Created", wireKey = "APPLICATION_CREATED", wireTag = 2),

        /** Fired when an application record is updated. */
        APPLICATION_UPDATED(displayName = "Application Updated", wireKey = "APPLICATION_UPDATED", wireTag = 3),

        /** Explicit human action via UI or CLI. */
        MANUAL(displayName = "Manual", wireKey = "MANUAL", wireTag = 4);
    }

    /** Associated-value carrier for unmapped trigger strings. */
    public data class Unrecognized(
        override val wireKey: String,
    ) : Trigger {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    /** Companion object providing lookup and shortcut accessors for [Trigger]. */
    public companion object {
        /** Default fallback value for [Trigger]. */
        public val DEFAULT: Trigger = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf("trigger:" + entry.name.lowercase().replace("_", "-"), entry.name.lowercase().replace("_", "-"))
        }

        /** Fallback when trigger source is unknown. */
        public val UNSPECIFIED: Trigger = Known.UNSPECIFIED
        /** Scheduled cron or timer job. */
        public val CRON: Trigger = Known.CRON
        /** Fired when an application record is created. */
        public val APPLICATION_CREATED: Trigger = Known.APPLICATION_CREATED
        /** Fired when an application record is updated. */
        public val APPLICATION_UPDATED: Trigger = Known.APPLICATION_UPDATED
        /** Explicit human action via UI or CLI. */
        public val MANUAL: Trigger = Known.MANUAL

        /** All canonical [Trigger] entries. */
        public val entries: List<Trigger> = Known.entries

        /**
         * Resolves a [Trigger] instance from its raw wire key string.
         *
         * @param raw Raw wire key string from external storage or serialized payload.
         * @return Matching canonical entry, or an unrecognized fallback instance.
         */
        public fun fromWireKey(raw: String?): Trigger {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }

        /** Parses a wire representation into [Trigger]. */
        public fun fromRemote(raw: String?): Trigger = fromWireKey(raw)
    }
}
