package dev.fnc314.tools.jobsearch.parser

/**
 * Normalizes legacy, human-entered, or unstructured status strings into canonical Firestore namespaced status keys.
 *
 * Provides status rank ordering for terminal UI list displays and identifies terminal application lifecycle states.
 */
public object StatusNormalizer {

    private val legacyStatusMap: Map<String, String> = mapOf(
        "draft" to "status:drafting",
        "drafting" to "status:drafting",
        "applied" to "status:applied",
        "screening" to "status:applied",
        "interview" to "status:interview",
        "interviewing" to "status:interview",
        "onsite" to "status:interview",
        "offer" to "status:offer",
        "hired" to "status:hired",
        "rejected" to "status:rejected",
        "no-response" to "status:no-response",
        "no_response" to "status:no-response",
        "offer-declined" to "status:offer-declined",
        "offer_declined" to "status:offer-declined",
        "withdrawn" to "status:withdrawn",
    )

    /**
     * Numerical sort ranking assigned to each canonical status key, with active progressing states ranked highest.
     */
    public val statusRank: Map<String, Int> = mapOf(
        "status:drafting" to 0,
        "status:applied" to 1,
        "status:interview" to 2,
        "status:offer" to 3,
        "status:hired" to 4,
        "status:offer-declined" to 5,
        "status:no-response" to 6,
        "status:rejected" to 7,
        "status:withdrawn" to 8,
    )

    /**
     * The set of canonical terminal statuses where the candidate's application cycle has completed.
     */
    public val terminalStatuses: Set<String> = setOf(
        "status:hired",
        "status:rejected",
        "status:offer-declined",
        "status:withdrawn",
    )

    /**
     * Maps an arbitrary raw status string to a valid canonical namespaced status identifier.
     *
     * Performs case-insensitive matching and whitespace trimming. Defaults to `"status:applied"` when unmapped.
     *
     * @param raw The raw status text to normalize.
     * @return The canonical namespaced status key (e.g., `"status:interview"`).
     */
    public fun normalizeStatus(raw: String): String {
        val trimmed = raw.trim().lowercase()
        return legacyStatusMap[trimmed] ?: "status:applied"
    }

    /**
     * Computes derived presentation properties for a given canonical status.
     *
     * @param status The canonical namespaced status key.
     * @return A map containing `"statusRank"` ([Int]) and `"isOpen"` ([Boolean]).
     */
    public fun deriveStatusFields(status: String): Map<String, Any> {
        return mapOf(
            "statusRank" to (statusRank[status] ?: 99),
            "isOpen" to (status !in terminalStatuses),
        )
    }
}
