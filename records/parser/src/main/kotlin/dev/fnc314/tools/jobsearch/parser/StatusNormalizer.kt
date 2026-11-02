package dev.fnc314.tools.jobsearch.parser

/**
 * Migrated from contract.py: normalizeStatus(), deriveStatusFields(),
 * getStatusRank(), getTerminalStatuses().
 */
object StatusNormalizer {

    // Legacy CSV status → namespaced status mapping
    private val legacyStatusMap = mapOf(
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

    // Status ranking for sorting
    val statusRank: Map<String, Int> = mapOf(
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

    // Terminal statuses (no further transitions expected)
    val terminalStatuses = setOf(
        "status:hired",
        "status:rejected",
        "status:offer-declined",
        "status:withdrawn",
    )

    /**
     * Map a legacy/human status string to a namespaced status value.
     */
    fun normalizeStatus(raw: String): String {
        val trimmed = raw.trim().lowercase()
        return legacyStatusMap[trimmed] ?: "status:applied"
    }

    /**
     * Compute derived status fields: statusRank and isOpen.
     */
    fun deriveStatusFields(status: String): Map<String, Any> {
        return mapOf(
            "statusRank" to (statusRank[status] ?: 99),
            "isOpen" to (status !in terminalStatuses),
        )
    }
}
