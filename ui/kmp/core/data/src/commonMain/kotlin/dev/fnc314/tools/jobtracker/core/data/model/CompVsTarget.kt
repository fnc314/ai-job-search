package dev.fnc314.tools.jobtracker.core.data.model

/** Compensation relative to target. Sealed: [Known] canonical values, [Unrecognized] verbatim carrier. */
public sealed interface CompVsTarget : WireContract {

    /** Closed vocabulary of known comparisons. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
    ) : CompVsTarget {
        /** Below target. */
        BELOW("Below", "below"),

        /** At target. */
        AT("At", "at"),

        /** Above target. */
        ABOVE("Above", "above");
    }

    /** Associated-value carrier for unmapped comparison strings. */
    public data class Unrecognized(override val wireKey: String) : CompVsTarget {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: CompVsTarget = Known.AT

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap()

        public val entries: List<CompVsTarget> = Known.entries

        public fun fromWireKey(raw: String?): CompVsTarget {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }
    }
}
