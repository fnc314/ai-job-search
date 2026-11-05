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

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

    /**
     * Closed vocabulary of known proposal and edit kinds.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : TrackerEditKind {
        /** Fallback when edit target is unspecified. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "proposal:unspecified", wireTag = 0),

        /** Proposal originating from Gmail synchronization. */
        EMAIL_SYNC(displayName = "Email Sync", wireKey = "proposal:gmail-sync", wireTag = 1),

        /** Proposal originating from Google Sheets synchronization. */
        SHEET_SYNC(displayName = "Sheet Sync", wireKey = "proposal:sheet-sync", wireTag = 2),

        /** Proposal originating from automated lead ingestion. */
        LEAD(displayName = "Lead", wireKey = "proposal:lead"),

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

    public companion object {
        public val DEFAULT: TrackerEditKind = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val aliases = mutableListOf<String>()
            aliases.add(entry.wireKey.substringAfter(':'))
            if (entry == Known.EMAIL_SYNC) {
                aliases.add("proposal:email-sync")
                aliases.add("email-sync")
            }
            aliases
        }

        public val UNSPECIFIED: TrackerEditKind = Known.UNSPECIFIED
        public val EMAIL_SYNC: TrackerEditKind = Known.EMAIL_SYNC
        public val SHEET_SYNC: TrackerEditKind = Known.SHEET_SYNC
        public val LEAD: TrackerEditKind = Known.LEAD
        public val STATUS: TrackerEditKind = Known.STATUS
        public val EVENT: TrackerEditKind = Known.EVENT
        public val SCORE: TrackerEditKind = Known.SCORE
        public val ARTIFACT: TrackerEditKind = Known.ARTIFACT
        public val NOTE: TrackerEditKind = Known.NOTE

        public val entries: List<TrackerEditKind> = Known.entries

        public fun fromWireKey(raw: String?): TrackerEditKind {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): TrackerEditKind = fromWireKey(raw)
    }
}
