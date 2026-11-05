package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Origination channel or Applicant Tracking System (ATS) platform through which an application was submitted.
 *
 * @property displayName Formatted name appropriate for display in application cards and filter pickers.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"channel:greenhouse"`).
 */
public enum class ApplicationChannel(
    public val displayName: String,
    public val firestoreValue: String,
) {
    /** Fallback when the sourcing vector is not documented. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "channel:unspecified"),

    /** Submitted through Greenhouse ATS. */
    GREENHOUSE(displayName = "Greenhouse", firestoreValue = "channel:greenhouse"),

    /** Submitted through Workday enterprise portal. */
    WORKDAY(displayName = "Workday", firestoreValue = "channel:workday"),

    /** Submitted through Lever ATS. */
    LEVER(displayName = "Lever", firestoreValue = "channel:lever"),

    /** Submitted through Ashby ATS. */
    ASHBY(displayName = "Ashby", firestoreValue = "channel:ashby"),

    /** Submitted through SmartRecruiters. */
    SMARTRECRUITERS(displayName = "SmartRecruiters", firestoreValue = "channel:smartrecruiters"),

    /** Sourced or applied through LinkedIn Jobs / InMail. */
    LINKEDIN(displayName = "LinkedIn", firestoreValue = "channel:linkedin"),

    /** Personal or employee referral. */
    REFERRAL(displayName = "Referral", firestoreValue = "channel:referral"),

    /** Direct outreach to founder, hiring manager, or company career portal. */
    DIRECT(displayName = "Direct", firestoreValue = "channel:direct"),

    /** Sourced through other portals or agency partners. */
    OTHER(displayName = "Other", firestoreValue = "channel:other");

    /** Returns the human-facing display label. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = firestoreValue

    public companion object {
        /**
         * Parses a raw channel string into an [ApplicationChannel] constant.
         *
         * Supports namespaced prefixes (`"channel:lever"`), enum names (`"LEVER"`), or simple names (`"lever"`).
         *
         * @param raw The raw string representation.
         * @return The matched [ApplicationChannel] or [UNSPECIFIED] if blank or unrecognized.
         */
        public fun fromFirestore(raw: String?): ApplicationChannel {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true) ||
                it.firestoreValue.removePrefix("channel:").equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }

        /**
         * Alias for [fromFirestore].
         *
         * @param raw The raw channel string.
         * @return The matched [ApplicationChannel] or [UNSPECIFIED].
         */
        public fun fromRaw(raw: String?): ApplicationChannel = fromFirestore(raw)
    }
}
