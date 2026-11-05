package dev.fnc314.tools.jobtracker.core.data.model

/** Geographic region qualifier for compensation tags. Sealed: [Known] canonical values, [Unrecognized] verbatim carrier. */
public sealed interface Region : WireContract {

    /** Closed vocabulary of known regions. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : Region {
        /** Fallback when region is unspecified. */
        UNSPECIFIED("Unspecified", "UNSPECIFIED", 0),

        /** Pittsburgh. */
        PIT("Pittsburgh", "PIT", 1),

        /** New York City. */
        NYC("New York City", "NYC", 2),

        /** San Francisco. */
        SF("San Francisco", "SF", 3),

        /** Los Angeles. */
        LA("Los Angeles", "LA", 4),

        /** Washington, D.C. */
        DC("Washington, D.C.", "DC", 5),

        /** Remote within the United States. */
        REMOTE_US("Remote (US)", "REMOTE-US", 6);
    }

    /** Associated-value carrier for unmapped region strings. */
    public data class Unrecognized(override val wireKey: String) : Region {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: Region = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap()

        public val entries: List<Region> = Known.entries

        public fun fromWireKey(raw: String?): Region {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }
    }
}
