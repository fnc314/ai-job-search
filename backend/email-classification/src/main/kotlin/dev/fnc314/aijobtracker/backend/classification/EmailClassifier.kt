package dev.fnc314.aijobtracker.backend.classification

import dev.fnc314.aijobtracker.backend.conventions.DocumentIdGenerator
import kotlinx.serialization.Serializable

/**
 * Result of classifying an email message.
 *
 * @property eventType Classified event type (e.g. RECEIVED, REJECT, SCREEN).
 * @property statusTo Target application status transition, or null if status is unaffected.
 * @property confidence Classification confidence level (e.g. HIGH_CONFIDENCE, MEDIUM_CONFIDENCE).
 */
@Serializable
public data class EmailClassificationResult(
    /** Classified event type (e.g. RECEIVED, REJECT, SCREEN). */
    public val eventType: String,
    /** Target application status transition, or null if status is unaffected. */
    public val statusTo: String?,
    /** Classification confidence level (e.g. HIGH_CONFIDENCE, MEDIUM_CONFIDENCE). */
    public val confidence: String,
)

/**
 * A staged change from email classification, ready for proposal review.
 *
 * @property applicationId Target application ID if matched, or null for candidate leads.
 * @property eventType Target event type string.
 * @property statusFrom Current application status prior to change, or null.
 * @property statusTo Target application status after change, or null.
 * @property confidence Classification confidence rating.
 * @property sourceEmail Extracted metadata for originating email.
 * @property leadCompany Company name if staged as a new application lead.
 * @property leadChannel Channel name if staged as a lead.
 */
@Serializable
public data class StagedEmailChange(
    /** Target application ID if matched, or null for candidate leads. */
    public val applicationId: String?,
    /** Target event type string. */
    public val eventType: String,
    /** Current application status prior to change, or null. */
    public val statusFrom: String?,
    /** Target application status after change, or null. */
    public val statusTo: String?,
    /** Classification confidence rating. */
    public val confidence: String,
    /** Extracted metadata for originating email. */
    public val sourceEmail: SourceEmail,
    /** Company name if staged as a new application lead. */
    public val leadCompany: String? = null,
    /** Channel name if staged as a lead. */
    public val leadChannel: String? = null,
)

/**
 * Email metadata extracted from a fetched message.
 *
 * @property messageId Unique email provider message ID.
 * @property subject Email subject line.
 * @property sender Sender email address string.
 * @property receivedAt ISO-8601 timestamp string when email was delivered.
 * @property snippet Text preview snippet from email body.
 */
@Serializable
public data class SourceEmail(
    /** Unique email provider message ID. */
    public val messageId: String,
    /** Email subject line. */
    public val subject: String?,
    /** Sender email address string. */
    public val sender: String?,
    /** ISO-8601 timestamp string when email was delivered. */
    public val receivedAt: String?,
    /** Text preview snippet from email body. */
    public val snippet: String? = null,
)

/**
 * An unmatched email message with a reason.
 *
 * @property sender Sender email address string.
 * @property subject Email subject line.
 * @property receivedAt ISO-8601 timestamp string when email was delivered.
 * @property reason Diagnostic reason why email could not be matched.
 * @property snippet Text preview snippet from email body.
 */
@Serializable
public data class UnmatchedEmail(
    /** Sender email address string. */
    public val sender: String?,
    /** Email subject line. */
    public val subject: String?,
    /** ISO-8601 timestamp string when email was delivered. */
    public val receivedAt: String?,
    /** Diagnostic reason why email could not be matched. */
    public val reason: String,
    /** Text preview snippet from email body. */
    public val snippet: String? = null,
)

/**
 * A single classifier rule: event type, optional status transition, confidence,
 * and a list of regex patterns. First match wins.
 *
 * @property eventType Target event type identifier.
 * @property statusTo Target status transition, or null.
 * @property confidence Match confidence rating.
 * @property patterns List of regular expression pattern strings.
 */
public data class EmailClassifierRule(
    /** Target event type identifier. */
    public val eventType: String,
    /** Target status transition, or null. */
    public val statusTo: String?,
    /** Match confidence rating. */
    public val confidence: String,
    /** List of regular expression pattern strings. */
    public val patterns: List<String>,
)

/**
 * Email classifier for classifying incoming email messages into application event types.
 *
 * Ordered by specificity: the first rule that matches wins, so a rejection that
 * also says "thank you for applying" is filed as a rejection.
 */
public object EmailClassifier {

    /**
     * Ordered cascade of email classification rules evaluated against email subject and body content.
     */
    public val CLASSIFIERS: List<EmailClassifierRule> = listOf(
        EmailClassifierRule(
            eventType = "REJECT",
            statusTo = "REJECTED",
            confidence = "HIGH_CONFIDENCE",
            patterns = listOf(
                "\\bunfortunately\\b",
                "not moving forward",
                "regret to inform",
                "no longer under consideration",
                "(?<!if you['’]re )(?<!if you are )\\bnot selected\\b",
                "won't be moving forward",
                "will not be moving forward",
                "decided to (?:move forward|proceed) with other",
                "application status",
                "position (?:has been|was) filled",
            ),
        ),
        EmailClassifierRule(
            eventType = "OFFER",
            statusTo = "OFFER_EXPECTED",
            confidence = "HIGH_CONFIDENCE",
            patterns = listOf(
                "\\bjob offer\\b",
                "pleased to offer",
                "offer letter",
                "we would like to offer you",
            ),
        ),
        EmailClassifierRule(
            eventType = "SCHEDULE_INTERVIEW",
            statusTo = "INTERVIEW",
            confidence = "MEDIUM_CONFIDENCE",
            patterns = listOf(
                "\\binterview\\b",
                "schedule (?:a )?(?:call|chat|conversation)",
                "phone screen",
                "hiring manager (?:call|conversation)",
            ),
        ),
        EmailClassifierRule(
            eventType = "ASSESS",
            statusTo = null,
            confidence = "MEDIUM_CONFIDENCE",
            patterns = listOf(
                "\\bcoding (?:challenge|assessment)\\b",
                "\\btake[- ]home\\b",
                "\\bonline assessment\\b",
                "\\btechnical assessment\\b",
            ),
        ),
        EmailClassifierRule(
            eventType = "ACKNOWLEDGE",
            statusTo = null,
            confidence = "LOW_CONFIDENCE",
            patterns = listOf(
                "thank you for applying",
                "application (?:has been |was )?received",
                "we(?:'ve| have) received your application",
                "thank you for your interest",
            ),
        ),
        EmailClassifierRule(
            eventType = "RECRUITER_OUTREACH",
            statusTo = null,
            confidence = "MEDIUM_CONFIDENCE",
            patterns = listOf(
                "came across your profile",
                "we have opportunities",
                "would you be interested",
                "exciting role",
                "reaching out because",
                "your background caught our attention",
                "impressed by your work",
                "saw your experience with",
                "we are looking for someone with your skills",
                "i'd like to discuss a potential opportunity",
            ),
        ),
    )

    /**
     * Classify an email message by subject and snippet.
     *
     * Returns an [EmailClassificationResult] if a pattern matches, or `null` if no
     * classifier rule matches.
     */
    public fun classify(subject: String?, snippet: String?): EmailClassificationResult? {
        val haystack = "${subject.orEmpty()} ${snippet.orEmpty()}".lowercase()
        for (rule in CLASSIFIERS) {
            for (pattern in rule.patterns) {
                val regex = try {
                    Regex(pattern, RegexOption.IGNORE_CASE)
                } catch (e: Exception) {
                    continue
                }
                if (regex.containsMatchIn(haystack)) {
                    return EmailClassificationResult(
                        eventType = rule.eventType,
                        statusTo = rule.statusTo,
                        confidence = rule.confidence,
                    )
                }
            }
        }
        return null
    }

    /**
     * Check whether the text contains a conservative match for the given company name.
     *
     * Matches the full company name as a word phrase, or all significant words
     * (length >= 5) from the slugified company name.
     */
    public fun companyMatches(text: String, application: Map<String, Any?>): Boolean {
        val company = (application["company"] as? String)?.lowercase()?.trim().orEmpty()
        if (company.isEmpty()) return false

        val escaped = Regex.escape(company)
        val fullMatchRegex = Regex("(?<![a-z0-9])$escaped(?![a-z0-9])", RegexOption.IGNORE_CASE)
        if (fullMatchRegex.containsMatchIn(text.lowercase())) return true

        val slugWords = DocumentIdGenerator.slug(company)
            .split("-")
            .filter { it.length >= 5 }
        if (slugWords.isEmpty()) return false

        val textLower = text.lowercase()
        return slugWords.all { word ->
            val wordRegex = Regex("(?<![a-z0-9])${Regex.escape(word)}(?![a-z0-9])", RegexOption.IGNORE_CASE)
            wordRegex.containsMatchIn(textLower)
        }
    }

    /**
     * Match a message to an application.
     *
     * Returns a [MatchResult] with either the matched application ID or a reason
     * for the mismatch.
     */
    public fun match(
        message: Map<String, Any?>,
        applications: Map<String, Map<String, Any?>>,
    ): MatchResult {
        val explicit = message["applicationId"] as? String
        if (!explicit.isNullOrBlank()) {
            return if (explicit in applications) {
                MatchResult(applicationId = explicit)
            } else {
                MatchResult(reason = "explicit applicationId '${explicit}' does not exist")
            }
        }

        val cleanSnippet = (message["snippet"] as? String).orEmpty()
            .replace(Regex("(?s)<style.*?>.*?</style>"), " ")
            .replace(Regex("(?s)<script.*?>.*?</script>"), " ")
            .replace(Regex("<[^>]+>"), " ")
        val haystack = "${(message["subject"] as? String).orEmpty()} ${(message["sender"] as? String).orEmpty()} $cleanSnippet".lowercase()
        val matches = applications.keys.filter { appId ->
            val app = applications[appId] ?: return@filter false
            companyMatches(haystack, app)
        }

        return when {
            matches.size == 1 -> MatchResult(applicationId = matches[0])
            matches.isEmpty() -> MatchResult(reason = "no application matched")
            else -> {
                // If multiple applications from the same company matched, try role-level disambiguation
                val roleMatches = matches.filter { appId ->
                    val app = applications[appId] ?: return@filter false
                    val role = (app["role"] as? String)?.lowercase()?.trim().orEmpty()
                    if (role.isEmpty()) return@filter false
                    // Look for distinctive tokens from the role name (excluding generic words)
                    val stopWords = setOf("senior", "staff", "principal", "lead", "engineer", "software", "developer", "mobile", "android", "ios", "at", "the", "and", "or", "in", "to", "for")
                    val tokens = role.split(Regex("[^a-z0-9]+")).filter { it.length >= 3 && it !in stopWords }
                    if (tokens.isNotEmpty()) {
                        tokens.all { token -> haystack.contains(token) }
                    } else {
                        // Fall back to title match
                        haystack.contains(role)
                    }
                }
                if (roleMatches.size == 1) {
                    MatchResult(applicationId = roleMatches[0])
                } else {
                    MatchResult(reason = "ambiguous: matched ${matches.size} applications (${matches.sorted().take(4).joinToString(", ")})")
                }
            }
        }
    }

    /**
     * Result of matching a message to an application.
     *
     * @property applicationId Matched application ID if successful, or null on failure.
     * @property reason Failure explanation if match could not be determined, or null on success.
     */
    public data class MatchResult(
        /** Matched application ID if successful, or null on failure. */
        public val applicationId: String? = null,
        /** Failure explanation if match could not be determined, or null on success. */
        public val reason: String? = null,
    ) {
        init {
            require((applicationId != null) xor (reason != null)) {
                "Exactly one of applicationId or reason must be set"
            }
        }
    }

    /**
     * Keywords that indicate a non-employer entity (recruitment agencies, staffing firms,
     * job board artifacts, generic HR phrases). If an extracted company name contains
     * any of these, it is rejected.
     */
    private val NON_EMPLOYER_KEYWORDS = setOf(
        "recruit", "recruitment", "staffing", "placements",
        "career poster", "career guidant", "talent acquisition",
        "human resources", "hr department", "hr team",
    )

    private val urlDomainPattern = Regex(".*\\.(com|org|net).*", RegexOption.IGNORE_CASE)
    private val hashPattern = Regex("^[0-9a-f]{16,}$", RegexOption.IGNORE_CASE)
    private val anonymousPostingPattern = Regex("major.*operator", RegexOption.IGNORE_CASE)

    /**
     * Try to extract a company name from an outreach email's sender or subject.
     *
     * Returns `null` if the extracted name looks like a URL, hash ID, recruitment agency,
     * generic HR phrase, or anonymous posting — preventing garbage company records.
     */
    public fun extractCompany(message: Map<String, Any?>): String? {
        val sender = (message["sender"] as? String).orEmpty().lowercase()
        val subject = (message["subject"] as? String).orEmpty().lowercase()
        val combined = "$sender $subject"

        val recruitingPatterns = listOf(
            "([a-z][a-z ]+?)\\s+(?:recruiting|recruitment|talent|hr|hiring|recruiter)".toRegex(RegexOption.IGNORE_CASE),
            "([a-z][a-z ]+?)\\s+(?:careers|jobs|opportunities)".toRegex(RegexOption.IGNORE_CASE),
        )

        for (pattern in recruitingPatterns) {
            val match = pattern.find(combined)
            match?.let {
                val company = it.groupValues[1].trim()

                // Title-case each word for proper company names
                val companyTitle = company.split(Regex("\\s+")).joinToString(" ") { word ->
                    word.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    }
                }

                if (companyTitle.length >= 2 && isValidCompanyName(companyTitle)) return companyTitle
            }
        }
        return null
    }

    /**
     * Validates an extracted company name against known garbage patterns.
     * Returns `false` for URLs, hash IDs, non-employer keywords, and anonymous postings.
     */
    private fun isValidCompanyName(name: String): Boolean {
        val nameLower = name.lowercase()

        // Reject URLs (e.g., "careers.dolby.com")
        if (urlDomainPattern.matches(nameLower)) return false

        // Reject hash-like strings (e.g., "a3c41b8b71eff8c4")
        if (hashPattern.matches(nameLower)) return false

        // Reject non-employer keywords (recruitment agencies, HR phrases, etc.)
        if (NON_EMPLOYER_KEYWORDS.any { keyword -> nameLower.contains(keyword) }) return false

        // Reject anonymous job postings (e.g., "major polish mobile telecom operator")
        if (anonymousPostingPattern.containsMatchIn(nameLower)) return false

        // Reject single-word results shorter than 3 characters
        val words = nameLower.split(Regex("\\s+"))
        if (words.size == 1 && words[0].length < 3) return false

        return true
    }
}
