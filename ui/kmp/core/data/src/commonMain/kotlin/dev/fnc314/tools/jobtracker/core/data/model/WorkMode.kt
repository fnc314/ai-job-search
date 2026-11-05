package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Remote work arrangement classification governing physical office attendance expectations.
 *
 * Implemented as a sealed interface hierarchy: [Known] holds canonical modes while
 * [Unrecognized] preserves unexpected wire strings verbatim.
 */
public sealed interface WorkMode : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

    /** Closed vocabulary of known work arrangements. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : WorkMode {
        /** Fallback when work arrangement is unstated. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "work-mode:unspecified", wireTag = 0),

        /** Full-time physical office attendance required. */
        ONSITE(displayName = "On-site", wireKey = "work-mode:onsite", wireTag = 1),

        /** Blended schedule with required in-office days and remote flexibility. */
        HYBRID(displayName = "Hybrid", wireKey = "work-mode:hybrid", wireTag = 2),

        /** 100% remote position with no mandatory office presence. */
        REMOTE(displayName = "Remote", wireKey = "work-mode:remote", wireTag = 3);
    }

    /** Associated-value carrier for unmapped work mode strings. */
    public data class Unrecognized(
        override val wireKey: String,
    ) : WorkMode {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: WorkMode = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf(entry.wireKey.removePrefix("work-mode:"))
        }

        public val UNSPECIFIED: WorkMode = Known.UNSPECIFIED
        public val ONSITE: WorkMode = Known.ONSITE
        public val HYBRID: WorkMode = Known.HYBRID
        public val REMOTE: WorkMode = Known.REMOTE

        public val entries: List<WorkMode> = Known.entries

        public fun fromWireKey(raw: String?): WorkMode {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): WorkMode = fromWireKey(raw)
    }
}
