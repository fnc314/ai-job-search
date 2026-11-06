package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Work arrangement and location modality of a role (on-site, hybrid, remote).
 *
 * Implemented as a sealed interface hierarchy: [Known] holds canonical modes while
 * [Unrecognized] preserves unexpected wire strings verbatim.
 */
public sealed interface RoleLocation : WireContract {

    /** Returns the display name. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

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

    public companion object {
        public val DEFAULT: RoleLocation = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            listOf(
                "work-mode:${entry.name.lowercase()}",
                entry.name.lowercase(),
                "work_${entry.name.lowercase()}",
                "work_${entry.name}",
            )
        }

        public val UNSPECIFIED: RoleLocation = Known.UNSPECIFIED
        public val ONSITE: RoleLocation = Known.ONSITE
        public val HYBRID: RoleLocation = Known.HYBRID
        public val REMOTE: RoleLocation = Known.REMOTE

        public val entries: List<RoleLocation> = Known.entries

        public fun fromWireKey(raw: String?): RoleLocation {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        public fun fromFirestore(raw: String?): RoleLocation = fromWireKey(raw)
    }
}

/** Backward-compatible typealias for [RoleLocation]. */
public typealias WorkMode = RoleLocation
