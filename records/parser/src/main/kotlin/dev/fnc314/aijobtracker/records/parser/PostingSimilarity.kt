package dev.fnc314.aijobtracker.records.parser

/**
 * Gate 0: pre-ingestion duplicate / ambiguity detection.
 *
 * Compares an incoming posting (company, role, URL) against already-tracked applications **before**
 * any record is created. Any returned match means the caller must block and route to the user.
 */
public object PostingSimilarity {
  /** Similarity at or above which a posting is considered ambiguous with an existing record. */
  public const val AMBIGUITY_THRESHOLD: Double = 0.75

  private val genericRoleTokens = setOf("engineer", "software", "developer", "senior", "staff", "principal", "lead")

  /** Minimal projection of an existing application used for comparison. */
  public data class Candidate(
    public val id: String,
    public val companyId: String,
    public val role: String,
    public val jobUrl: String?,
    public val status: String,
  )

  /** A candidate that collides with the incoming posting. */
  public data class Match(
    public val candidate: Candidate,
    public val score: Double,
    public val reason: String,
  )

  /**
   * Returns existing applications that are identical or similar to the incoming posting, best first.
   */
  public fun findSimilar(
    company: String,
    role: String,
    url: String?,
    existing: List<Candidate>,
    threshold: Double = AMBIGUITY_THRESHOLD,
  ): List<Match> {
    val companyId = DocumentIdGenerator.slug(company)
    val incomingId = runCatching { DocumentIdGenerator.documentId(company, role) }.getOrNull()
    val incomingUrl = normalizeUrl(url)
    val incomingTokens = roleTokens(role)

    return existing
      .mapNotNull { candidate ->
        when {
          incomingUrl != null && incomingUrl == normalizeUrl(candidate.jobUrl) ->
            Match(candidate, 1.0, "same posting URL")
          incomingId != null && candidate.id == incomingId ->
            Match(candidate, 1.0, "same document id")
          companyId.isNotEmpty() && candidate.companyId == companyId -> {
            val score = jaccard(incomingTokens, roleTokens(candidate.role))
            if (score >= threshold) Match(candidate, score, "similar role at same company") else null
          }
          else -> null
        }
      }.sortedByDescending { it.score }
  }

  internal fun roleTokens(role: String): Set<String> {
    val tokens = DocumentIdGenerator.normalizeDirName(role).split("-").filter { it.isNotBlank() }.toSet()
    val specific = tokens - genericRoleTokens
    return specific.ifEmpty { tokens }
  }

  private fun jaccard(
    a: Set<String>,
    b: Set<String>,
  ): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0
    return a.intersect(b).size.toDouble() / a.union(b).size.toDouble()
  }

  private fun normalizeUrl(url: String?): String? =
    url
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?.substringBefore('?')
      ?.substringBefore('#')
      ?.removeSuffix("/")
      ?.lowercase()
}
