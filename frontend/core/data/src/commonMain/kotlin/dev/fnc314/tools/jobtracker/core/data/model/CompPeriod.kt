package dev.fnc314.tools.jobtracker.core.data.model

/** Compensation period. Sealed: [Known] canonical values, [Unrecognized] verbatim carrier. */
public sealed interface CompPeriod : WireContract {

    /** Closed vocabulary of known periods. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
    ) : CompPeriod {
        /** Annual. */
        YEAR("Year", "year"),

        /** Monthly. */
        MONTH("Month", "month"),

        /** Hourly. */
        HOUR("Hour", "hour");
    }

    /** Associated-value carrier for unmapped period strings. */
    public data class Unrecognized(override val wireKey: String) : CompPeriod {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    /** Companion object providing lookup and shortcut accessors for [CompPeriod]. */
    public companion object {
        /** Default fallback value for [CompPeriod]. */
        public val DEFAULT: CompPeriod = Known.YEAR

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap()

        /** All canonical [CompPeriod] entries. */
        public val entries: List<CompPeriod> = Known.entries

        /**
         * Resolves a [CompPeriod] instance from its raw wire key string.
         *
         * @param raw Raw wire key string from external storage or serialized payload.
         * @return Matching canonical entry, or an unrecognized fallback instance.
         */
        public fun fromWireKey(raw: String?): CompPeriod {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }
    }
}
