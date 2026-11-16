package dev.fnc314.aijobtracker.frontend.core.data.model

/** Geographic region qualifier for compensation tags. Sealed: [Known] canonical values, [Unrecognized] verbatim carrier. */
public sealed interface Region : WireContract {

    /** Closed vocabulary of known regions. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : Region {
        /** Fallback when region is unspecified. */
        UNSPECIFIED("Unspecified", "UNSPECIFIED", wireTag = 0),

        /** Pittsburgh. */
        US_PIT("Pittsburgh", "US_PIT", wireTag = 1),

        /** New York City. */
        US_NYC("New York City", "US_NYC", wireTag = 2),

        /** San Francisco. */
        US_SF("San Francisco", "US_SF", wireTag = 3),

        /** Los Angeles. */
        US_LA("Los Angeles", "US_LA", wireTag = 4),

        /** Washington, D.C. */
        US_DC("Washington, D.C.", "US_DC", wireTag = 5),

        /** Remote within the United States. */
        US_REMOTE("Remote (US)", "US_REMOTE", wireTag = 6);
    }

    /** Associated-value carrier for unmapped region strings. */
    public data class Unrecognized(override val wireKey: String) : Region {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    /** Companion object providing lookup and shortcut accessors for [Region]. */
    public companion object {
        /** Default fallback value for [Region]. */
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
        /** Shortcut accessor for [Known.US_PIT]. */
        public val PIT: Region = Known.US_PIT
        /** Shortcut accessor for [Known.US_NYC]. */
        public val NYC: Region = Known.US_NYC
        /** Shortcut accessor for [Known.US_SF]. */
        public val SF: Region = Known.US_SF
        /** Shortcut accessor for [Known.US_LA]. */
        public val LA: Region = Known.US_LA
        /** Shortcut accessor for [Known.US_DC]. */
        public val DC: Region = Known.US_DC
        /** Shortcut accessor for [Known.US_REMOTE]. */
        public val REMOTE_US: Region = Known.US_REMOTE

        /** All canonical [Region] entries. */
        public val entries: List<Region> = Known.entries

        /**
         * Resolves a [Region] instance from its raw wire key string.
         *
         * @param raw Raw wire key string from external storage or serialized payload.
         * @return Matching canonical entry, or an unrecognized fallback instance.
         */
        public fun fromWireKey(raw: String?): Region {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }
    }
}
