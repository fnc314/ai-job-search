package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Review and adjudication state of a staged [TrackerEdit] proposal.
 *
 * @property displayName Formatted name for UI display.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"proposal:pending"`).
 */
public enum class TrackerEditState(
    public val displayName: String,
    public val firestoreValue: String,
) {
    /** Fallback when proposal state is unrecorded. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "proposal:unspecified"),

    /** Staged and awaiting explicit candidate review and approval. */
    PENDING(displayName = "Pending", firestoreValue = "proposal:pending"),

    /** Confirmed by candidate; mutations applied to the target application record. */
    APPROVED(displayName = "Approved", firestoreValue = "proposal:approved"),

    /** Dismissed by candidate; proposal rejected with no underlying mutations made. */
    REJECTED(displayName = "Rejected", firestoreValue = "proposal:rejected"),

    /** Unreviewed proposal expired after its time-to-live deadline passed. */
    EXPIRED(displayName = "Expired", firestoreValue = "proposal:expired");

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = firestoreValue

    public companion object {
        /**
         * Resolves a raw or namespaced string into a [TrackerEditState] enum.
         *
         * @param raw The raw string representation.
         * @return The matched [TrackerEditState] or [UNSPECIFIED].
         */
        public fun fromFirestore(raw: String?): TrackerEditState {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
