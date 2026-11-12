package dev.fnc314.tools.jobsearch.parser

/**
 * Compile-time exhaustive Kotlin sealed hierarchy modeling canonical job application lifecycle states.
 *
 * Separates [Terminal] inactive and success outcomes from [Active] progressing funnel stages.
 * [Active.Interviewing] carries strongly typed [InterviewProgress], guaranteeing null-safe pattern matching.
 */
public sealed interface JobApplicationStatus {
    public val wireName: String
    public val rank: Int
    public val isTerminal: Boolean

    /**
     * Terminal lifecycle states (ranks 0-3 for inactive terminal, rank 11 for success).
     */
    public sealed interface Terminal : JobApplicationStatus {
        override val isTerminal: Boolean get() = true

        public data object NoResponse : Terminal {
            override val wireName: String = "NO_RESPONSE"
            override val rank: Int = 0
        }

        public data object OfferDeclined : Terminal {
            override val wireName: String = "OFFER_DECLINED"
            override val rank: Int = 1
        }

        public data object ApplicationWithdrawn : Terminal {
            override val wireName: String = "APPLICATION_WITHDRAWN"
            override val rank: Int = 2
        }

        public data object Rejected : Terminal {
            override val wireName: String = "REJECTED"
            override val rank: Int = 3
        }

        public data object Hired : Terminal {
            override val wireName: String = "HIRED"
            override val rank: Int = 11
        }
    }

    /**
     * Active funnel stages (ranks 4-10 in chronological sequence).
     */
    public sealed interface Active : JobApplicationStatus {
        override val isTerminal: Boolean get() = false

        public data object Drafted : Active {
            override val wireName: String = "DRAFTED"
            override val rank: Int = 4
        }

        public data object Submitted : Active {
            override val wireName: String = "SUBMITTED"
            override val rank: Int = 5
        }

        public data object RecruiterScreened : Active {
            override val wireName: String = "RECRUITER_SCREENED"
            override val rank: Int = 6
        }

        public data class Interviewing(
            val progress: InterviewProgress? = null,
        ) : Active {
            override val wireName: String = "INTERVIEWING"
            override val rank: Int = 7
        }

        public data object NegotiatingPreOffer : Active {
            override val wireName: String = "NEGOTIATING_PRE_OFFER"
            override val rank: Int = 8
        }

        public data object Offer : Active {
            override val wireName: String = "OFFER"
            override val rank: Int = 9
        }

        public data object NegotiatingPostOffer : Active {
            override val wireName: String = "NEGOTIATING_POST_OFFER"
            override val rank: Int = 10
        }
    }

    public companion object {
        public fun parse(raw: String, interviewProgress: InterviewProgress? = null): JobApplicationStatus {
            val normalized = StatusNormalizer.normalizeStatus(raw)
            return when (normalized) {
                Terminal.NoResponse.wireName -> Terminal.NoResponse
                Terminal.OfferDeclined.wireName -> Terminal.OfferDeclined
                Terminal.ApplicationWithdrawn.wireName -> Terminal.ApplicationWithdrawn
                Terminal.Rejected.wireName -> Terminal.Rejected
                Terminal.Hired.wireName -> Terminal.Hired
                Active.Drafted.wireName -> Active.Drafted
                Active.Submitted.wireName -> Active.Submitted
                Active.RecruiterScreened.wireName -> Active.RecruiterScreened
                "INTERVIEWING" -> Active.Interviewing(interviewProgress)
                Active.NegotiatingPreOffer.wireName -> Active.NegotiatingPreOffer
                Active.Offer.wireName -> Active.Offer
                Active.NegotiatingPostOffer.wireName -> Active.NegotiatingPostOffer
                else -> Active.Submitted
            }
        }
    }
}
