package dev.fnc314.tools.jobtracker.core.data.model

/** Controlled-vocabulary categorization tag. Sealed: [Known] canonical values, [Unrecognized] verbatim carrier. */
public sealed interface Tag : WireContract {

    /** Closed vocabulary of known tags. */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : Tag {
        /** Fallback when tag is unspecified. */
        UNSPECIFIED("Unspecified", "tag:unspecified", 0),

        /** Relocation is required. */
        RELOCATION_REQUIRED("Relocation Required", "relocation:required", 1),

        /** Relocation was accepted. */
        RELOCATION_ACCEPTED("Relocation Accepted", "relocation:accepted", 2),

        /** Relocation was declined. */
        RELOCATION_DECLINED("Relocation Declined", "relocation:declined", 3),

        /** Compensation above the NYC super-commuter threshold. */
        COMPENSATION_ABOVE_SUPER_COMMUTER_NYC("Above Super-Commuter (NYC)", "compensation:above-super-commuter:NYC", 4),

        /** Compensation above the SF super-commuter threshold. */
        COMPENSATION_ABOVE_SUPER_COMMUTER_SF("Above Super-Commuter (SF)", "compensation:above-super-commuter:SF", 5),

        /** Compensation below the NYC floor. */
        COMPENSATION_BELOW_NYC_FLOOR("Below NYC Floor", "compensation:below-nyc-floor", 6),

        /** Compensation below the target band. */
        COMPENSATION_BELOW_BAND("Below Band", "compensation:below-band", 7),

        /** Compensation at the target band. */
        COMPENSATION_AT_BAND("At Band", "compensation:at-band", 8),

        /** Compensation above the target band. */
        COMPENSATION_ABOVE_BAND("Above Band", "compensation:above-band", 9),

        /** Compensation weighted toward equity. */
        COMPENSATION_EQUITY_HEAVY("Equity Heavy", "compensation:equity-heavy", 10),

        /** Strong fit. */
        FIT_STRONG("Strong Fit", "fit:strong", 11),

        /** Stretch fit. */
        FIT_STRETCH("Stretch Fit", "fit:stretch", 12),

        /** iOS experience gap. */
        FIT_GAP_IOS("iOS Gap", "fit:gap-ios", 13),

        /** Cross-platform experience gap. */
        FIT_GAP_CROSS_PLATFORM("Cross-Platform Gap", "fit:gap-cross-platform", 14),

        /** Process includes a take-home. */
        PROCESS_TAKE_HOME("Take-Home", "process:take-home", 15),

        /** Process originated from a referral. */
        PROCESS_REFERRAL("Referral", "process:referral", 16),

        /** Process has an internal contact. */
        PROCESS_INTERNAL_CONTACT("Internal Contact", "process:internal-contact", 17),

        /** No response for 30+ days. */
        PROCESS_NO_RESPONSE_30D("No Response (30d)", "process:no-response-30d", 18),

        /** Recruiter-initiated outreach. */
        PROCESS_RECRUITER_OUTREACH("Recruiter Outreach", "process:recruiter:outreach");
    }

    /** Associated-value carrier for unmapped tag strings. */
    public data class Unrecognized(override val wireKey: String) : Tag {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: Tag = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap()

        public val entries: List<Tag> = Known.entries

        public fun fromWireKey(raw: String?): Tag {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }
    }
}
