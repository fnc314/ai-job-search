package dev.fnc314.tools.jobtracker.core.data.model

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

    public companion object {
        public val DEFAULT: SyncKind = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            if (entry == Known.EMAIL_COLLECT) listOf("sync:gmail-collect", "sync:email-collect", "email-collect", "gmail-collect") else emptyList()
        }

        public val entries: List<SyncKind> = Known.entries

        public fun fromWireKey(raw: String?): SyncKind {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }
    }
}
