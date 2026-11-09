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
        UNSPECIFIED("Unspecified", "tag:unspecified", wireTag = 0),

        /** Relocation is required. */
        RELOCATION_REQUIRED("Relocation Required", "relocation:required", wireTag = 1),

        /** Relocation was accepted. */
        RELOCATION_ACCEPTED("Relocation Accepted", "relocation:accepted", wireTag = 2),

        /** Relocation was declined. */
        RELOCATION_DECLINED("Relocation Declined", "relocation:declined", wireTag = 3),

        /** Compensation above the NYC super-commuter threshold. */
        COMPENSATION_ABOVE_SUPER_COMMUTER_NYC("Above Super-Commuter (NYC)", "compensation:above-super-commuter:NYC", wireTag = 4),

        /** Compensation above the SF super-commuter threshold. */
        COMPENSATION_ABOVE_SUPER_COMMUTER_SF("Above Super-Commuter (SF)", "compensation:above-super-commuter:SF", wireTag = 5),

        /** Compensation below the NYC floor. */
        COMPENSATION_BELOW_NYC_FLOOR("Below NYC Floor", "compensation:below-nyc-floor", wireTag = 6),

        /** Compensation below the target band. */
        COMPENSATION_BELOW_BAND("Below Band", "compensation:below-band", wireTag = 7),

        /** Compensation at the target band. */
        COMPENSATION_AT_BAND("At Band", "compensation:at-band", wireTag = 8),

        /** Compensation above the target band. */
        COMPENSATION_ABOVE_BAND("Above Band", "compensation:above-band", wireTag = 9),

        /** Compensation weighted toward equity. */
        COMPENSATION_EQUITY_HEAVY("Equity Heavy", "compensation:equity-heavy", wireTag = 10),

        /** Strong fit. */
        FIT_STRONG("Strong Fit", "fit:strong", wireTag = 11),

        /** Stretch fit. */
        FIT_STRETCH("Stretch Fit", "fit:stretch", wireTag = 12),

        /** iOS experience gap. */
        FIT_GAP_IOS("iOS Gap", "fit:gap-ios", wireTag = 13),

        /** Cross-platform experience gap. */
        FIT_GAP_CROSS_PLATFORM("Cross-Platform Gap", "fit:gap-cross-platform", wireTag = 14),

        /** Process includes a take-home. */
        PROCESS_TAKE_HOME("Take-Home", "process:take-home", wireTag = 15),

        /** Process originated from a referral. */
        PROCESS_REFERRAL("Referral", "process:referral", wireTag = 16),

        /** Process has an internal contact. */
        PROCESS_INTERNAL_CONTACT("Internal Contact", "process:internal-contact", wireTag = 17),

        /** No response for 30+ days. */
        PROCESS_NO_RESPONSE_30D("No Response (30d)", "process:no-response-30d", wireTag = 18),

        /** Recruiter-initiated outreach. */
        PROCESS_RECRUITER_OUTREACH("Recruiter Outreach", "process:recruiter:outreach");
    }

    /** Associated-value carrier for unmapped tag strings. */
    public data class Unrecognized(override val wireKey: String) : Tag {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: Tag = Known.UNSPECIFIED

        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val legacy = when (entry) {
                Known.RELOCATION_REQUIRED -> "relocation:required"
                Known.RELOCATION_ACCEPTED -> "relocation:accepted"
                Known.RELOCATION_DECLINED -> "relocation:declined"
                Known.COMPENSATION_ABOVE_SUPER_COMMUTER_NYC -> "compensation:above-super-commuter:NYC"
                Known.COMPENSATION_ABOVE_SUPER_COMMUTER_SF -> "compensation:above-super-commuter:SF"
                Known.COMPENSATION_BELOW_NYC_FLOOR -> "compensation:below-nyc-floor"
                Known.COMPENSATION_BELOW_BAND -> "compensation:below-band"
                Known.COMPENSATION_AT_BAND -> "compensation:at-band"
                Known.COMPENSATION_ABOVE_BAND -> "compensation:above-band"
                Known.COMPENSATION_EQUITY_HEAVY -> "compensation:equity-heavy"
                Known.FIT_STRONG -> "fit:strong"
                Known.FIT_STRETCH -> "fit:stretch"
                Known.FIT_GAP_IOS -> "fit:gap-ios"
                Known.FIT_GAP_CROSS_PLATFORM -> "fit:gap-cross-platform"
                Known.PROCESS_TAKE_HOME -> "process:take-home"
                Known.PROCESS_REFERRAL -> "process:referral"
                Known.PROCESS_INTERNAL_CONTACT -> "process:internal-contact"
                Known.PROCESS_NO_RESPONSE_30D -> "process:no-response-30d"
                Known.PROCESS_RECRUITER_OUTREACH -> "process:recruiter:outreach"
                Known.UNSPECIFIED -> "tag:unspecified"
            }
            listOf(legacy, legacy.lowercase(), entry.name.lowercase())
        }

        public val entries: List<Tag> = Known.entries

        public fun fromWireKey(raw: String?): Tag {
            if (raw.isNullOrBlank()) return DEFAULT
            return lookup[raw.trim().lowercase()] ?: Unrecognized(raw)
        }
    }
}
