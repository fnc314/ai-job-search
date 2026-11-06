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

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

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

    public companion object {
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
        public val UNSPECIFIED: ApplicationChannel = Known.UNSPECIFIED
        public val GREENHOUSE: ApplicationChannel = Known.GREENHOUSE
        public val WORKDAY: ApplicationChannel = Known.WORKDAY
        public val LEVER: ApplicationChannel = Known.LEVER
        public val ASHBY: ApplicationChannel = Known.ASHBY
        public val SMARTRECRUITERS: ApplicationChannel = Known.SMARTRECRUITERS
        public val LINKEDIN: ApplicationChannel = Known.LINKEDIN
        public val REFERRAL: ApplicationChannel = Known.REFERRAL
        public val DIRECT: ApplicationChannel = Known.DIRECT
        public val OUTREACH_LINKEDIN: ApplicationChannel = Known.OUTREACH_LINKEDIN
        public val OUTREACH_EMAIL: ApplicationChannel = Known.OUTREACH_EMAIL
        public val OUTREACH_REFERRAL: ApplicationChannel = Known.OUTREACH_REFERRAL
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

        public fun fromFirestore(raw: String?): ApplicationChannel = fromWireKey(raw)
        public fun fromRaw(raw: String?): ApplicationChannel = fromWireKey(raw)
    }
}
