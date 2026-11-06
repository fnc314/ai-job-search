package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Review and adjudication state of a staged [TrackerEdit] proposal.
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates canonical proposal states, while [Unrecognized] captures unexpected strings.
 */
public sealed interface TrackerEditState : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

    /**
     * Closed vocabulary of known proposal review states.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : TrackerEditState {
        /** Fallback when proposal state is unrecorded. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "UNSPECIFIED", wireTag = 0),

        /** Staged and awaiting explicit candidate review and approval. */
        PENDING(displayName = "Pending", wireKey = "PENDING", wireTag = 1),

        /** Confirmed by candidate; mutations applied to the target application record. */
        APPROVED(displayName = "Approved", wireKey = "APPROVED", wireTag = 2),

        /** Dismissed by candidate; proposal rejected with no underlying mutations made. */
        REJECTED(displayName = "Rejected", wireKey = "REJECTED", wireTag = 3),

        /** Unreviewed proposal expired after its time-to-live deadline passed. */
        EXPIRED(displayName = "Expired", wireKey = "EXPIRED", wireTag = 4);
    }

    /**
     * Associated-value carrier for unmapped, future, or custom edit state strings.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : TrackerEditState {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: TrackerEditState = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf("proposal:" + entry.name.lowercase(), entry.name.lowercase())
        }

        public val UNSPECIFIED: TrackerEditState = Known.UNSPECIFIED
        public val PENDING: TrackerEditState = Known.PENDING
        public val APPROVED: TrackerEditState = Known.APPROVED
        public val REJECTED: TrackerEditState = Known.REJECTED
        public val EXPIRED: TrackerEditState = Known.EXPIRED

        public val entries: List<TrackerEditState> = Known.entries

        public fun fromWireKey(raw: String?): TrackerEditState {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): TrackerEditState = fromWireKey(raw)
    }
}
