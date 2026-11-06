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
        US_PIT("Pittsburgh", "US_PIT", 1),

        /** New York City. */
        US_NYC("New York City", "US_NYC", 2),

        /** San Francisco. */
        US_SF("San Francisco", "US_SF", 3),

        /** Los Angeles. */
        US_LA("Los Angeles", "US_LA", 4),

        /** Washington, D.C. */
        US_DC("Washington, D.C.", "US_DC", 5),

        /** Remote within the United States. */
        US_REMOTE("Remote (US)", "US_REMOTE", 6);
    }

    /** Associated-value carrier for unmapped region strings. */
    public data class Unrecognized(override val wireKey: String) : Region {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: Region = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val naked = entry.name.removePrefix("US_").lowercase()
            listOf(
                naked,
                naked.replace("_", "-"),
                entry.name.lowercase(),
                entry.name.lowercase().replace("_", "-")
            )
        }

        // Aliases for backward compatibility
        public val PIT: Region = Known.US_PIT
        public val NYC: Region = Known.US_NYC
        public val SF: Region = Known.US_SF
        public val LA: Region = Known.US_LA
        public val DC: Region = Known.US_DC
        public val REMOTE_US: Region = Known.US_REMOTE

        public val entries: List<Region> = Known.entries

        public fun fromWireKey(raw: String?): Region {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }
    }
}
