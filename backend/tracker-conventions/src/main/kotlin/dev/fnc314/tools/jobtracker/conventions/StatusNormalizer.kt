package dev.fnc314.tools.jobtracker.conventions

/**
 * Normalizes legacy, human-entered, or unstructured status strings into canonical Firestore status keys.
 *
 * Provides status rank ordering for terminal UI list displays and identifies terminal application lifecycle states.
 */
public object StatusNormalizer {

    private val canonicalStatuses: Set<String> = setOf(
        "NO_RESPONSE",
        "OFFER_DECLINED",
        "APPLICATION_WITHDRAWN",
        "REJECTED",
        "DRAFTED",
        "SUBMITTED",
        "RECRUITER_SCREENED",
        "INTERVIEWING",
        "NEGOTIATING_PRE_OFFER",
        "OFFER",
        "NEGOTIATING_POST_OFFER",
        "HIRED",
    )

    private val legacyStatusMap: Map<String, String> = mapOf(
        "draft" to "DRAFTED",
        "drafting" to "DRAFTED",
        "status:drafting" to "DRAFTED",
        "drafted" to "DRAFTED",
        "lead" to "DRAFTED",
        "status:lead" to "DRAFTED",
        "applied" to "SUBMITTED",
        "status:applied" to "SUBMITTED",
        "submitted" to "SUBMITTED",
        "screening" to "RECRUITER_SCREENED",
        "recruiter_screen" to "RECRUITER_SCREENED",
        "recruiter_screened" to "RECRUITER_SCREENED",
        "recruiter-screen" to "RECRUITER_SCREENED",
        "recruiter-screened" to "RECRUITER_SCREENED",
        "interview" to "INTERVIEWING",
        "interviewing" to "INTERVIEWING",
        "status:interview" to "INTERVIEWING",
        "onsite" to "INTERVIEWING",
        "negotiating_pre_offer" to "NEGOTIATING_PRE_OFFER",
        "negotiating-pre-offer" to "NEGOTIATING_PRE_OFFER",
        "offer" to "OFFER",
        "offer_expected" to "OFFER",
        "status:offer" to "OFFER",
        "negotiating_post_offer" to "NEGOTIATING_POST_OFFER",
        "negotiating-post-offer" to "NEGOTIATING_POST_OFFER",
        "hired" to "HIRED",
        "status:hired" to "HIRED",
        "rejected" to "REJECTED",
        "status:rejected" to "REJECTED",
        "no-response" to "NO_RESPONSE",
        "no_response" to "NO_RESPONSE",
        "no response" to "NO_RESPONSE",
        "status:no-response" to "NO_RESPONSE",
        "offer-declined" to "OFFER_DECLINED",
        "offer_declined" to "OFFER_DECLINED",
        "offer declined" to "OFFER_DECLINED",
        "status:offer-declined" to "OFFER_DECLINED",
        "withdrawn" to "APPLICATION_WITHDRAWN",
        "application_withdrawn" to "APPLICATION_WITHDRAWN",
        "application-withdrawn" to "APPLICATION_WITHDRAWN",
        "status:withdrawn" to "APPLICATION_WITHDRAWN",
    )

    /**
     * Numerical sort ranking assigned to each canonical status key (ranks 0-11).
     * Terminal inactive states (ranks 0-3), active funnel states (ranks 4-10), terminal success (rank 11).
     */
    @Suppress("MagicNumber")
    public val statusRank: Map<String, Int> = mapOf(
        "NO_RESPONSE" to 0,
        "OFFER_DECLINED" to 1,
        "APPLICATION_WITHDRAWN" to 2,
        "REJECTED" to 3,
        "DRAFTED" to 4,
        "SUBMITTED" to 5,
        "RECRUITER_SCREENED" to 6,
        "INTERVIEWING" to 7,
        "NEGOTIATING_PRE_OFFER" to 8,
        "OFFER" to 9,
        "NEGOTIATING_POST_OFFER" to 10,
        "HIRED" to 11,
        // Legacy support
        "LEAD" to 4,
        "DRAFTING" to 4,
        "APPLIED" to 5,
        "INTERVIEW" to 7,
        "OFFER_EXPECTED" to 9,
        "status:no-response" to 0,
        "status:offer-declined" to 1,
        "status:withdrawn" to 2,
        "status:rejected" to 3,
        "status:lead" to 4,
        "status:drafting" to 4,
        "status:applied" to 5,
        "status:interview" to 7,
        "status:offer" to 9,
        "status:hired" to 11,
    )

    /**
     * The set of canonical terminal statuses where the candidate's application cycle has completed.
     */
    public val terminalStatuses: Set<String> = setOf(
        "NO_RESPONSE",
        "OFFER_DECLINED",
        "APPLICATION_WITHDRAWN",
        "REJECTED",
        "HIRED",
        // Legacy support
        "status:no-response",
        "status:offer-declined",
        "status:withdrawn",
        "status:rejected",
        "status:hired",
    )

    /**
     * Maps an arbitrary raw status string to a valid canonical status identifier.
     *
     * Performs case-insensitive matching and whitespace trimming. Defaults to `"SUBMITTED"` when unmapped.
     *
     * @param raw The raw status text to normalize.
     * @return The canonical status key (e.g., `"INTERVIEWING"`).
     */
    public fun normalizeStatus(raw: String): String {
        val trimmed = raw.trim()
        val upper = trimmed.uppercase()
        if (upper in canonicalStatuses) return upper
        val lower = trimmed.lowercase()
        return legacyStatusMap[lower] ?: "SUBMITTED"
    }

    private const val DEFAULT_STATUS_RANK: Int = 99

    /**
     * Computes derived presentation properties for a given canonical status.
     *
     * @param status The canonical status key.
     * @return A map containing `"statusRank"` ([Int]) and `"isOpen"` ([Boolean]).
     */
    public fun deriveStatusFields(status: String): Map<String, Any> {
        val normalized = normalizeStatus(status)
        return mapOf(
            "statusRank" to (statusRank[normalized] ?: DEFAULT_STATUS_RANK),
            "isOpen" to (normalized !in terminalStatuses),
        )
    }
}
