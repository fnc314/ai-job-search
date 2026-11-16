package dev.fnc314.aijobtracker.frontend.core.data.model

/**
 * Work arrangement and location modality of a role (on-site, hybrid, remote).
 *
 * Implemented as a sealed interface hierarchy: [Known] holds canonical modes while
 * [Unrecognized] preserves unexpected wire strings verbatim.
 */
public sealed interface RoleLocation : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical remote document string representation. */
    public fun toRemote(): String = wireKey

    /** Closed vocabulary of known role location modalities. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : RoleLocation {
        /** Fallback when work arrangement is unstated. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "UNSPECIFIED", wireTag = 0),

        /** Full-time physical office attendance required. */
        ONSITE(displayName = "On-site", wireKey = "ONSITE", wireTag = 1),

        /** Blended schedule with required in-office days and remote flexibility. */
        HYBRID(displayName = "Hybrid", wireKey = "HYBRID", wireTag = 2),

        /** 100% remote position with no mandatory office presence. */
        REMOTE(displayName = "Remote", wireKey = "REMOTE", wireTag = 3);
    }

    /** Associated-value carrier for unmapped role location strings. */
    public data class Unrecognized(
        override val wireKey: String,
    ) : RoleLocation {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    /** Companion object providing lookup and shortcut accessors for [RoleLocation]. */
    public companion object {
        /** Default fallback value for [RoleLocation]. */
        public val DEFAULT: RoleLocation = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf(
                "work-mode:${entry.name.lowercase()}",
                entry.name.lowercase(),
                "work_${entry.name.lowercase()}",
                "work_${entry.name}",
            )
        }

        /** Fallback when work arrangement is unstated. */
        public val UNSPECIFIED: RoleLocation = Known.UNSPECIFIED
        /** Full-time physical office attendance required. */
        public val ONSITE: RoleLocation = Known.ONSITE
        /** Blended schedule with required in-office days and remote flexibility. */
        public val HYBRID: RoleLocation = Known.HYBRID
        /** 100% remote position with no mandatory office presence. */
        public val REMOTE: RoleLocation = Known.REMOTE

        /** All canonical [RoleLocation] entries. */
        public val entries: List<RoleLocation> = Known.entries

        /**
         * Resolves a [RoleLocation] instance from its raw wire key string.
         *
         * @param raw Raw wire key string from external storage or serialized payload.
         * @return Matching canonical entry, or an unrecognized fallback instance.
         */
        public fun fromWireKey(raw: String?): RoleLocation {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        /** Parses a wire representation into [RoleLocation]. */
        public fun fromRemote(raw: String?): RoleLocation = fromWireKey(raw)
    }
}

/** Backward-compatible typealias for [RoleLocation]. */
public typealias WorkMode = RoleLocation
