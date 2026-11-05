package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Trigger source responsible for initiating an automated workflow or edit proposal.
 *
 * Implemented as a sealed interface hierarchy: [Known] holds canonical triggers while
 * [Unrecognized] preserves unexpected wire strings verbatim.
 */
public sealed interface Trigger : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

    /** Closed vocabulary of known trigger sources. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : Trigger {
        /** Fallback when trigger source is unknown. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "trigger:unspecified", wireTag = 0),

        /** Scheduled cron or timer job. */
        CRON(displayName = "Cron", wireKey = "trigger:cron", wireTag = 1),

        /** Fired when an application record is created. */
        APPLICATION_CREATED(displayName = "Application Created", wireKey = "trigger:application-created", wireTag = 2),

        /** Fired when an application record is updated. */
        APPLICATION_UPDATED(displayName = "Application Updated", wireKey = "trigger:application-updated", wireTag = 3),

        /** Explicit human action via UI or CLI. */
        MANUAL(displayName = "Manual", wireKey = "trigger:manual", wireTag = 4);
    }

    /** Associated-value carrier for unmapped trigger strings. */
    public data class Unrecognized(
        override val wireKey: String,
    ) : Trigger {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: Trigger = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf(entry.wireKey.removePrefix("trigger:"))
        }

        public val UNSPECIFIED: Trigger = Known.UNSPECIFIED
        public val CRON: Trigger = Known.CRON
        public val APPLICATION_CREATED: Trigger = Known.APPLICATION_CREATED
        public val APPLICATION_UPDATED: Trigger = Known.APPLICATION_UPDATED
        public val MANUAL: Trigger = Known.MANUAL

        public val entries: List<Trigger> = Known.entries

        public fun fromWireKey(raw: String?): Trigger {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): Trigger = fromWireKey(raw)
    }
}
