package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Origination channel or Applicant Tracking System (ATS) platform through which an application was submitted.
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates all compile-time known ATS channels and outreach vectors, while [Unrecognized]
 * captures arbitrary or forward-compatible channel strings without data loss.
 */
public sealed interface ApplicationChannel : WireContract {

    /** Returns the human-facing display label. */
    public fun displayName(): String = displayName

    /** Returns the canonical remote document string representation. */
    public fun toRemote(): String = wireKey

    /**
     * Closed vocabulary of known application channels.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : ApplicationChannel {
        /** Fallback when the sourcing vector is not documented. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "UNSPECIFIED", wireTag = 0),

        /** Submitted through Greenhouse ATS. */
        GREENHOUSE(displayName = "Greenhouse", wireKey = "GREENHOUSE", wireTag = 1),

        /** Submitted through Workday enterprise portal. */
        WORKDAY(displayName = "Workday", wireKey = "WORKDAY", wireTag = 2),

        /** Submitted through Lever ATS. */
        LEVER(displayName = "Lever", wireKey = "LEVER", wireTag = 3),

        /** Submitted through Ashby ATS. */
        ASHBY(displayName = "Ashby", wireKey = "ASHBY", wireTag = 4),

        /** Submitted through SmartRecruiters. */
        SMARTRECRUITERS(displayName = "SmartRecruiters", wireKey = "SMARTRECRUITERS", wireTag = 5),

        /** Sourced or applied through LinkedIn Jobs / InMail. */
        LINKEDIN(displayName = "LinkedIn", wireKey = "LINKEDIN", wireTag = 6),

        /** Personal or employee referral. */
        REFERRAL(displayName = "Referral", wireKey = "REFERRAL", wireTag = 7),

        /** Direct outreach to founder, hiring manager, or company career portal. */
        DIRECT(displayName = "Direct", wireKey = "DIRECT", wireTag = 8),

        /** Direct recruiter outreach via LinkedIn. */
        OUTREACH_LINKEDIN(displayName = "Outreach (LinkedIn)", wireKey = "OUTREACH_LINKEDIN"),

        /** Direct recruiter outreach via Email. */
        OUTREACH_EMAIL(displayName = "Outreach (Email)", wireKey = "OUTREACH_EMAIL"),

        /** Direct outreach via Referral. */
        OUTREACH_REFERRAL(displayName = "Outreach (Referral)", wireKey = "OUTREACH_REFERRAL"),

        /** Sourced through other portals or agency partners. */
        OTHER(displayName = "Other", wireKey = "OTHER", wireTag = 9);
    }

    /**
     * Associated-value carrier for unmapped, future, or custom channel strings.
     * Preserves the verbatim incoming [wireKey] without loss of fidelity.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : ApplicationChannel {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    /** Companion object providing lookup and shortcut accessors for [ApplicationChannel]. */
    public companion object {
        /** Default fallback value for [ApplicationChannel]. */
        public val DEFAULT: ApplicationChannel = Known.UNSPECIFIED

        // Static O(1) multi-key lookup map indexing wireKey, name, and bare channel suffix
        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val nameLower = entry.name.lowercase()
            val kebab = nameLower.replace("_", "-")
            listOf(
                "channel:$kebab",
                "channel:${kebab.replace("outreach-", "outreach:")}",
                kebab,
                nameLower,
            )
        }

        // Backward-compatible shortcut properties matching standard enum names
        /** Fallback when the sourcing vector is not documented. */
        public val UNSPECIFIED: ApplicationChannel = Known.UNSPECIFIED
        /** Submitted through Greenhouse ATS. */
        public val GREENHOUSE: ApplicationChannel = Known.GREENHOUSE
        /** Submitted through Workday enterprise portal. */
        public val WORKDAY: ApplicationChannel = Known.WORKDAY
        /** Submitted through Lever ATS. */
        public val LEVER: ApplicationChannel = Known.LEVER
        /** Submitted through Ashby ATS. */
        public val ASHBY: ApplicationChannel = Known.ASHBY
        /** Submitted through SmartRecruiters. */
        public val SMARTRECRUITERS: ApplicationChannel = Known.SMARTRECRUITERS
        /** Sourced or applied through LinkedIn Jobs / InMail. */
        public val LINKEDIN: ApplicationChannel = Known.LINKEDIN
        /** Personal or employee referral. */
        public val REFERRAL: ApplicationChannel = Known.REFERRAL
        /** Direct outreach to founder, hiring manager, or company career portal. */
        public val DIRECT: ApplicationChannel = Known.DIRECT
        /** Direct recruiter outreach via LinkedIn. */
        public val OUTREACH_LINKEDIN: ApplicationChannel = Known.OUTREACH_LINKEDIN
        /** Direct recruiter outreach via Email. */
        public val OUTREACH_EMAIL: ApplicationChannel = Known.OUTREACH_EMAIL
        /** Direct outreach via Referral. */
        public val OUTREACH_REFERRAL: ApplicationChannel = Known.OUTREACH_REFERRAL
        /** Sourced through other portals or agency partners. */
        public val OTHER: ApplicationChannel = Known.OTHER

        /** All known canonical entries for pickers, dropdowns, and filters. */
        public val entries: List<ApplicationChannel> = Known.entries

        /**
         * Resolves a raw channel string into an [ApplicationChannel] instance.
         *
         * Supports namespaced prefixes (`"channel:lever"`), enum names (`"LEVER"`), or simple names (`"lever"`).
         *
         * @param raw The raw string representation.
         * @return The matched [Known] channel or [Unrecognized] containing the raw string.
         */
        public fun fromWireKey(raw: String?): ApplicationChannel {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        /** Parses a wire representation into [ApplicationChannel]. */
        public fun fromRemote(raw: String?): ApplicationChannel = fromWireKey(raw)
        /** Parses a wire representation into [ApplicationChannel]. */
        public fun fromRaw(raw: String?): ApplicationChannel = fromWireKey(raw)
    }
}
