package dev.fnc314.tools.jobsearch.parser

import kotlinx.serialization.Serializable

/**
 * Result of classifying an email message.
 *
 * Mirrors the Python `proposals.py` classifier output.
 */
@Serializable
public data class ClassificationResult(
    val eventType: String,
    val statusTo: String?,
    val confidence: String,
)

/**
 * A staged change from email classification, ready for proposal review.
 */
@Serializable
public data class StagedChange(
    val applicationId: String?,
    val eventType: String,
    val statusFrom: String?,
    val statusTo: String?,
    val confidence: String,
    val sourceEmail: SourceEmail,
    val leadCompany: String? = null,
    val leadChannel: String? = null,
)

/**
 * Email metadata extracted from a fetched message.
 */
@Serializable
public data class SourceEmail(
    val messageId: String,
    val subject: String?,
    val sender: String?,
    val receivedAt: String?,
    val snippet: String? = null,
)

/**
 * An unmatched email message with a reason.
 */
@Serializable
public data class UnmatchedMessage(
    val sender: String?,
    val subject: String?,
    val receivedAt: String?,
    val reason: String,
    val snippet: String? = null,
)

/**
 * A single classifier rule: event type, optional status transition, confidence,
 * and a list of regex patterns. First match wins.
 */
public data class ClassifierRule(
    val eventType: String,
    val statusTo: String?,
    val confidence: String,
    val patterns: List<String>,
)

/**
 * Email classifier mirroring Python `proposals.py`.
 *
 * Ordered by specificity: the first rule that matches wins, so a rejection that
 * also says "thank you for applying" is filed as a rejection.
 *
 * @deprecated Use [dev.fnc314.tools.jobtracker.classification.EmailClassifier] instead.
 * This class is superseded by the email-classification module.
 */
@Deprecated(
    message = "Use EmailClassifier from the email-classification module instead.",
    replaceWith = ReplaceWith(
        "EmailClassifier",
        "dev.fnc314.tools.jobtracker.classification.EmailClassifier"
    )
)
public object ProposalClassifier {

    public val CLASSIFIERS: List<ClassifierRule> = listOf(
        ClassifierRule(
            eventType = "REJECT",
            statusTo = "REJECTED",
            confidence = "HIGH_CONFIDENCE",
            patterns = listOf(
                "\\bunfortunately\\b",
                "not moving forward",
                "regret to inform",
                "no longer under consideration",
                "\\bnot selected\\b",
                "won't be moving forward",
                "will not be moving forward",
                "decided to (?:move forward|proceed) with other",
                "application status",
                "position (?:has been|was) filled",
            ),
        ),
        ClassifierRule(
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
        ClassifierRule(
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
        ClassifierRule(
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
        ClassifierRule(
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
        ClassifierRule(
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
     * Returns a [ClassificationResult] if a pattern matches, or `null` if no
     * classifier rule matches.
     */
    public fun classify(subject: String?, snippet: String?): ClassificationResult? {
        val haystack = "${subject.orEmpty()} ${snippet.orEmpty()}".lowercase()
        for (rule in CLASSIFIERS) {
            for (pattern in rule.patterns) {
                val regex = try {
                    Regex(pattern, RegexOption.IGNORE_CASE)
                } catch (e: Exception) {
                    continue
                }
                if (regex.containsMatchIn(haystack)) {
                    return ClassificationResult(
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

        val haystack = "${(message["subject"] as? String).orEmpty()} ${(message["sender"] as? String).orEmpty()} ${(message["snippet"] as? String).orEmpty()}".lowercase()
        val matches = applications.keys.filter { appId ->
            val app = applications[appId] ?: return@filter false
            companyMatches(haystack, app)
        }

        return when {
            matches.size == 1 -> MatchResult(applicationId = matches[0])
            matches.isEmpty() -> MatchResult(reason = "no application matched")
            else -> MatchResult(reason = "ambiguous: matched ${matches.size} applications (${matches.sorted().take(4).joinToString(", ")})")
        }
    }

    /**
     * Result of matching a message to an application.
     */
    public data class MatchResult(
        val applicationId: String? = null,
        val reason: String? = null,
    ) {
        init {
            require((applicationId != null) xor (reason != null)) {
                "Exactly one of applicationId or reason must be set"
            }
        }
    }

    /**
     * Try to extract a company name from an outreach email's sender or subject.
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
                val company = it.groupValues[1].trim().replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
                if (company.length >= 2) return company
            }
        }
        return null
    }
}
