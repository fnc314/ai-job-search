package dev.fnc314.tools.jobsearch.parser

/**
 * Normalizes legacy, human-entered, or unstructured status strings into canonical Firestore status keys.
 *
 * Provides status rank ordering for terminal UI list displays and identifies terminal application lifecycle states.
 */
public object StatusNormalizer {

    private val legacyStatusMap: Map<String, String> = mapOf(
        "draft" to "DRAFTING",
        "drafting" to "DRAFTING",
        "status:drafting" to "DRAFTING",
        "lead" to "LEAD",
        "status:lead" to "LEAD",
        "applied" to "APPLIED",
        "status:applied" to "APPLIED",
        "screening" to "APPLIED",
        "interview" to "INTERVIEW",
        "interviewing" to "INTERVIEW",
        "status:interview" to "INTERVIEW",
        "onsite" to "INTERVIEW",
        "offer" to "OFFER_EXPECTED",
        "offer_expected" to "OFFER_EXPECTED",
        "status:offer" to "OFFER_EXPECTED",
        "hired" to "HIRED",
        "status:hired" to "HIRED",
        "rejected" to "REJECTED",
        "status:rejected" to "REJECTED",
        "no-response" to "NO_RESPONSE",
        "no_response" to "NO_RESPONSE",
        "status:no-response" to "NO_RESPONSE",
        "offer-declined" to "OFFER_DECLINED",
        "offer_declined" to "OFFER_DECLINED",
        "status:offer-declined" to "OFFER_DECLINED",
        "withdrawn" to "APPLICATION_WITHDRAWN",
        "application_withdrawn" to "APPLICATION_WITHDRAWN",
        "status:withdrawn" to "APPLICATION_WITHDRAWN",
    )

    /**
     * Numerical sort ranking assigned to each canonical status key, with active progressing states ranked highest.
     */
    @Suppress("MagicNumber")
    public val statusRank: Map<String, Int> = mapOf(
        "LEAD" to 0,
        "DRAFTING" to 1,
        "APPLIED" to 2,
        "INTERVIEW" to 3,
        "OFFER_EXPECTED" to 4,
        "HIRED" to 5,
        "OFFER_DECLINED" to 6,
        "NO_RESPONSE" to 7,
        "REJECTED" to 8,
        "APPLICATION_WITHDRAWN" to 9,
        // Legacy support
        "status:lead" to 0,
        "status:drafting" to 1,
        "status:applied" to 2,
        "status:interview" to 3,
        "status:offer" to 4,
        "status:hired" to 5,
        "status:offer-declined" to 6,
        "status:no-response" to 7,
        "status:rejected" to 8,
        "status:withdrawn" to 9,
    )

    /**
     * The set of canonical terminal statuses where the candidate's application cycle has completed.
     */
    public val terminalStatuses: Set<String> = setOf(
        "HIRED",
        "REJECTED",
        "OFFER_DECLINED",
        "APPLICATION_WITHDRAWN",
        // Legacy support
        "status:hired",
        "status:rejected",
        "status:offer-declined",
        "status:withdrawn",
    )

    /**
     * Maps an arbitrary raw status string to a valid canonical status identifier.
     *
     * Performs case-insensitive matching and whitespace trimming. Defaults to `"APPLIED"` when unmapped.
     *
     * @param raw The raw status text to normalize.
     * @return The canonical status key (e.g., `"INTERVIEW"`).
     */
    public fun normalizeStatus(raw: String): String {
        val trimmed = raw.trim().lowercase()
        return legacyStatusMap[trimmed] ?: "APPLIED"
    }

    private const val DEFAULT_STATUS_RANK: Int = 99

    /**
     * Computes derived presentation properties for a given canonical status.
     *
     * @param status The canonical status key.
     * @return A map containing `"statusRank"` ([Int]) and `"isOpen"` ([Boolean]).
     */
    public fun deriveStatusFields(status: String): Map<String, Any> {
        return mapOf(
            "statusRank" to (statusRank[status] ?: DEFAULT_STATUS_RANK),
            "isOpen" to (status !in terminalStatuses),
        )
    }
}
