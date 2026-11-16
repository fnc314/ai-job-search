package dev.fnc314.aijobtracker.frontend.core.data.model

/** Synchronization operation type. Sealed: [Known] canonical values, [Unrecognized] verbatim carrier. */
public sealed interface SyncKind : WireContract {

    /** Closed vocabulary of known sync kinds. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : SyncKind {
        /** Fallback when sync kind is unspecified. */
        UNSPECIFIED("Unspecified", "sync:unspecified", 0),

        /** Gmail message collection run. */
        EMAIL_COLLECT("Email Collect", "sync:gmail-collect", 1);
    }

    /** Associated-value carrier for unmapped sync kind strings. */
    public data class Unrecognized(override val wireKey: String) : SyncKind {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    /** Companion object providing lookup and shortcut accessors for [SyncKind]. */
    public companion object {
        /** Default fallback value for [SyncKind]. */
        public val DEFAULT: SyncKind = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            if (entry == Known.EMAIL_COLLECT) listOf("sync:gmail-collect", "sync:email-collect", "email-collect", "gmail-collect") else emptyList()
        }

        /** All canonical [SyncKind] entries. */
        public val entries: List<SyncKind> = Known.entries

        /**
         * Resolves a [SyncKind] instance from its raw wire key string.
         *
         * @param raw Raw wire key string from external storage or serialized payload.
         * @return Matching canonical entry, or an unrecognized fallback instance.
         */
        public fun fromWireKey(raw: String?): SyncKind {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }
    }
}
