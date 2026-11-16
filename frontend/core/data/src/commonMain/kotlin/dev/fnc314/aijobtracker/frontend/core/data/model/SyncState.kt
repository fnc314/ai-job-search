package dev.fnc314.aijobtracker.frontend.core.data.model

/** Sync request lifecycle state. Sealed: [Known] canonical values, [Unrecognized] verbatim carrier. */
public sealed interface SyncState : WireContract {

    /** Closed vocabulary of known sync states. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : SyncState {
        /** Fallback when state is unspecified. */
        UNSPECIFIED("Unspecified", "sync:unspecified", wireTag = 0),

        /** Queued and awaiting a worker. */
        PENDING("Pending", "sync:pending", wireTag = 1),

        /** Claimed by a worker and in flight. */
        CLAIMED("Claimed", "sync:claimed", wireTag = 2),

        /** Completed successfully. */
        DONE("Done", "sync:done", wireTag = 3),

        /** Terminated with an error. */
        FAILED("Failed", "sync:failed", wireTag = 4);
    }

    /** Associated-value carrier for unmapped sync state strings. */
    public data class Unrecognized(override val wireKey: String) : SyncState {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    /** Companion object providing lookup and shortcut accessors for [SyncState]. */
    public companion object {
        /** Default fallback value for [SyncState]. */
        public val DEFAULT: SyncState = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf("sync:" + entry.name.lowercase(), entry.name.lowercase())
        }

        /** All canonical [SyncState] entries. */
        public val entries: List<SyncState> = Known.entries

        /**
         * Resolves a [SyncState] instance from its raw wire key string.
         *
         * @param raw Raw wire key string from external storage or serialized payload.
         * @return Matching canonical entry, or an unrecognized fallback instance.
         */
        public fun fromWireKey(raw: String?): SyncState {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }
    }
}
