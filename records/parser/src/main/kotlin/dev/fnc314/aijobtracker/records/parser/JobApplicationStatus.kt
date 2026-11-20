package dev.fnc314.aijobtracker.records.parser

/**
 * Compile-time exhaustive Kotlin sealed hierarchy modeling canonical job application lifecycle states.
 *
 * Separates [Terminal] inactive and success outcomes from [Active] progressing funnel stages.
 * [Active.Interviewing] carries strongly typed [InterviewProgress], guaranteeing null-safe pattern matching.
 */
public sealed interface JobApplicationStatus {
  /** Canonical protobuf/wire string representation of this status. */
  public val wireName: String

  /** Funnel progression ordinal used for sorting and comparison. */
  public val rank: Int

  /** True if this status represents an inactive or final terminal outcome. */
  public val isTerminal: Boolean

  /**
   * Terminal lifecycle states (ranks 0-3 for inactive terminal, rank 11 for success).
   */
  public sealed interface Terminal : JobApplicationStatus {
    override val isTerminal: Boolean get() = true

    /** Candidate received no response from employer. */
    public data object NoResponse : Terminal {
      override val wireName: String = "NO_RESPONSE"
      override val rank: Int = 0
    }

    /** Candidate formally declined an extended job offer. */
    public data object OfferDeclined : Terminal {
      override val wireName: String = "OFFER_DECLINED"
      override val rank: Int = 1
    }

    /** Candidate withdrew the application from consideration. */
    public data object ApplicationWithdrawn : Terminal {
      override val wireName: String = "APPLICATION_WITHDRAWN"
      override val rank: Int = 2
    }

    /** Application was rejected by the employer. */
    public data object Rejected : Terminal {
      override val wireName: String = "REJECTED"
      override val rank: Int = 3
    }

    /** Candidate was offered and accepted the position (successful hire). */
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

    /** Scraped and deduplicated, not yet ranked. Neutral fallback for unrecognized input. */
    public data object Scraped : Active {
      override val wireName: String = StatusNormalizer.UNSPECIFIED_STATUS
      override val rank: Int = 4
    }

    /** Initial application preparation before submission. */
    public data object Drafted : Active {
      override val wireName: String = "DRAFTED"
      override val rank: Int = 4
    }

    /** Async artifact generation or active drafting in progress. */
    public data object Drafting : Active {
      override val wireName: String = "DRAFTING"
      override val rank: Int = 4
    }

    /** Job posting evaluated by RankScorer, awaiting artifact generation decision. */
    public data object Ranked : Active {
      override val wireName: String = "RANKED"
      override val rank: Int = 4
    }

    /** Application formally submitted to the hiring organization. */
    public data object Submitted : Active {
      override val wireName: String = "SUBMITTED"
      override val rank: Int = 5
    }

    /** Initial recruiter screening call scheduled or completed. */
    public data object RecruiterScreened : Active {
      override val wireName: String = "RECRUITER_SCREENED"
      override val rank: Int = 6
    }

    /**
     * Active interviewing lifecycle stage with optional round progress.
     *
     * @property progress Strongly typed round progress and notes.
     */
    public data class Interviewing(
      public val progress: InterviewProgress? = null,
    ) : Active {
      override val wireName: String = "INTERVIEWING"
      override val rank: Int = 7
    }

    /** Pre-offer compensation, role, or team negotiation in progress. */
    public data object NegotiatingPreOffer : Active {
      override val wireName: String = "NEGOTIATING_PRE_OFFER"
      override val rank: Int = 8
    }

    /** Formal offer extended by employer. */
    public data object Offer : Active {
      override val wireName: String = "OFFER"
      override val rank: Int = 9
    }

    /** Post-offer terms or counter-offer negotiation in progress. */
    public data object NegotiatingPostOffer : Active {
      override val wireName: String = "NEGOTIATING_POST_OFFER"
      override val rank: Int = 10
    }
  }

  /** Parser and factory utility for [JobApplicationStatus]. */
  public companion object {
    /**
     * Parses a raw status string into the canonical [JobApplicationStatus] instance.
     *
     * @param raw Raw status text or wire name.
     * @param interviewProgress Optional [InterviewProgress] when status is interviewing.
     * @return Resolved [JobApplicationStatus].
     */
    public fun parse(
      raw: String,
      interviewProgress: InterviewProgress? = null,
    ): JobApplicationStatus {
      val normalized = StatusNormalizer.normalizeStatus(raw)
      return when (normalized) {
        Terminal.NoResponse.wireName -> Terminal.NoResponse
        Terminal.OfferDeclined.wireName -> Terminal.OfferDeclined
        Terminal.ApplicationWithdrawn.wireName -> Terminal.ApplicationWithdrawn
        Terminal.Rejected.wireName -> Terminal.Rejected
        Terminal.Hired.wireName -> Terminal.Hired
        Active.Drafted.wireName -> Active.Drafted
        Active.Drafting.wireName -> Active.Drafting
        Active.Ranked.wireName -> Active.Ranked
        Active.Submitted.wireName -> Active.Submitted
        Active.RecruiterScreened.wireName -> Active.RecruiterScreened
        "INTERVIEWING" -> Active.Interviewing(interviewProgress)
        Active.NegotiatingPreOffer.wireName -> Active.NegotiatingPreOffer
        Active.Offer.wireName -> Active.Offer
        Active.NegotiatingPostOffer.wireName -> Active.NegotiatingPostOffer
        else -> Active.Scraped
      }
    }
  }
}