package dev.fnc314.tools.jobtracker.core.data.model

public enum class ApplicationChannel(
    public val displayName: String,
    public val firestoreValue: String,
) {
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "channel:unspecified"),
    GREENHOUSE(displayName = "Greenhouse", firestoreValue = "channel:greenhouse"),
    WORKDAY(displayName = "Workday", firestoreValue = "channel:workday"),
    LEVER(displayName = "Lever", firestoreValue = "channel:lever"),
    ASHBY(displayName = "Ashby", firestoreValue = "channel:ashby"),
    SMARTRECRUITERS(displayName = "SmartRecruiters", firestoreValue = "channel:smartrecruiters"),
    LINKEDIN(displayName = "LinkedIn", firestoreValue = "channel:linkedin"),
    REFERRAL(displayName = "Referral", firestoreValue = "channel:referral"),
    DIRECT(displayName = "Direct", firestoreValue = "channel:direct"),
    OTHER(displayName = "Other", firestoreValue = "channel:other");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public companion object {
        public fun fromFirestore(raw: String?): ApplicationChannel {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            val cleaned = raw.trim()
            return entries.firstOrNull {
                it.firestoreValue.equals(cleaned, ignoreCase = true) ||
                it.name.equals(cleaned, ignoreCase = true) ||
                it.firestoreValue.removePrefix("channel:").equals(cleaned, ignoreCase = true)
            } ?: UNSPECIFIED
        }

        public fun fromRaw(raw: String?): ApplicationChannel = fromFirestore(raw)
    }
}
