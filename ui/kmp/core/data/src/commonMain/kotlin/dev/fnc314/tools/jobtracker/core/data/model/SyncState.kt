package dev.fnc314.tools.jobtracker.core.data.model

/** Sync request lifecycle state. Sealed: [Known] canonical values, [Unrecognized] verbatim carrier. */
public sealed interface SyncState : WireContract {

    /** Closed vocabulary of known sync states. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : SyncState {
        /** Fallback when state is unspecified. */
        UNSPECIFIED("Unspecified", "sync:unspecified", 0),

        /** Queued and awaiting a worker. */
        PENDING("Pending", "sync:pending", 1),

        /** Claimed by a worker and in flight. */
        CLAIMED("Claimed", "sync:claimed", 2),

        /** Completed successfully. */
        DONE("Done", "sync:done", 3),

        /** Terminated with an error. */
        FAILED("Failed", "sync:failed", 4);
    }

    /** Associated-value carrier for unmapped sync state strings. */
    public data class Unrecognized(override val wireKey: String) : SyncState {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: SyncState = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf("sync:" + entry.name.lowercase(), entry.name.lowercase())
        }

        public val entries: List<SyncState> = Known.entries

        public fun fromWireKey(raw: String?): SyncState {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }
    }
}
