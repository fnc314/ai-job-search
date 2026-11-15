package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Categorical scope of a proposed modification within a [TrackerEdit].
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates canonical edit kinds, while [Unrecognized] captures unexpected strings.
 */
public sealed interface TrackerEditKind : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical remote document string representation. */
    public fun toRemote(): String = wireKey

    /**
     * Closed vocabulary of known proposal and edit kinds.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : TrackerEditKind {
        /** Fallback when edit target is unspecified. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "UNSPECIFIED", wireTag = 0),

        /** Proposal originating from Gmail synchronization. */
        EMAIL_SYNC(displayName = "Email Sync", wireKey = "EMAIL_SYNC", wireTag = 1),

        /** Proposal originating from Google Sheets synchronization. */
        SHEET_SYNC(displayName = "Sheet Sync", wireKey = "SHEET_SYNC", wireTag = 2),

        /** Proposal originating from automated lead ingestion. */
        LEAD(displayName = "Lead", wireKey = "LEAD"),

        /** Status transition update (e.g. from Applied to Interview). */
        STATUS(displayName = "Status", wireKey = "edit:status"),

        /** Addition of an audit event to the application timeline. */
        EVENT(displayName = "Event", wireKey = "edit:event"),

        /** Recalculation or update of the candidate-to-job fit score. */
        SCORE(displayName = "Score", wireKey = "edit:score"),

        /** Attachment or update of a document artifact reference. */
        ARTIFACT(displayName = "Artifact", wireKey = "edit:artifact"),

        /** Addition or amendment of application strategy notes. */
        NOTE(displayName = "Note", wireKey = "edit:note");
    }

    /**
     * Associated-value carrier for unmapped, future, or custom edit kind strings.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : TrackerEditKind {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    /** Companion object providing lookup and shortcut accessors for [TrackerEditKind]. */
    public companion object {
        /** Default fallback value for [TrackerEditKind]. */
        public val DEFAULT: TrackerEditKind = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val aliases = mutableListOf<String>(
                "proposal:" + entry.name.lowercase().replace("_", "-"),
                entry.name.lowercase().replace("_", "-")
            )
            if (entry == Known.EMAIL_SYNC) {
                aliases.add("proposal:gmail-sync")
                aliases.add("proposal:email-sync")
                aliases.add("gmail-sync")
                aliases.add("email-sync")
            }
            aliases
        }

        /** Fallback when edit target is unspecified. */
        public val UNSPECIFIED: TrackerEditKind = Known.UNSPECIFIED
        /** Proposal originating from Gmail synchronization. */
        public val EMAIL_SYNC: TrackerEditKind = Known.EMAIL_SYNC
        /** Proposal originating from Google Sheets synchronization. */
        public val SHEET_SYNC: TrackerEditKind = Known.SHEET_SYNC
        /** Proposal originating from automated lead ingestion. */
        public val LEAD: TrackerEditKind = Known.LEAD
        /** Status transition update (e.g. from Applied to Interview). */
        public val STATUS: TrackerEditKind = Known.STATUS
        /** Addition of an audit event to the application timeline. */
        public val EVENT: TrackerEditKind = Known.EVENT
        /** Recalculation or update of the candidate-to-job fit score. */
        public val SCORE: TrackerEditKind = Known.SCORE
        /** Attachment or update of a document artifact reference. */
        public val ARTIFACT: TrackerEditKind = Known.ARTIFACT
        /** Addition or amendment of application strategy notes. */
        public val NOTE: TrackerEditKind = Known.NOTE

        /** All canonical [TrackerEditKind] entries. */
        public val entries: List<TrackerEditKind> = Known.entries

        /**
         * Resolves a [TrackerEditKind] instance from its raw wire key string.
         *
         * @param raw Raw wire key string from external storage or serialized payload.
         * @return Matching canonical entry, or an unrecognized fallback instance.
         */
        public fun fromWireKey(raw: String?): TrackerEditKind {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        /** Parses a wire representation into [TrackerEditKind]. */
        public fun fromRemote(raw: String?): TrackerEditKind = fromWireKey(raw)
    }
}
