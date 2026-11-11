package dev.fnc314.tools.jobsearch.parser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Result of a keyword-based job ranking evaluation.
 *
 * Mirrors the Python `rank.py` output structure.
 */
@Serializable
public data class RankResult(
    val overall: Int,
    val verdict: String,
    val technical: Int,
    val experience: Int,
    val behavioral: Int,
    val career: Int,
    val deal_breakers: List<String>,
    val strengths: List<String>,
    val gaps: List<String>,
    val company: String,
    val role: String,
)

/**
 * Simple container for extracted job info.
 */
public data class JobInfo(val company: String, val role: String)

/**
 * Keyword-based job fit scorer mirroring Python `rank.py`.
 *
 * Unlike [FitEvaluator], which uses a base-score-plus-adjustments model grounded in
 * the candidate profile, this scorer uses keyword matching across four weighted
 * dimensions: technical, experience, behavioral, and career.
 */
public object RankScorer {

    // === Scoring Weights ===

    private val WEIGHTS = mapOf(
        "technical" to 0.30,
        "experience" to 0.25,
        "behavioral" to 0.15,
        "career" to 0.30,
    )

    // === Keyword Lists ===

    public val STRONG_TECH_KEYWORDS: List<String> = listOf(
        "kotlin", "android", "jetpack compose", "kmp", "kotlin multiplatform",
        "gradle", "mobile architecture", "mobile architect", "devex",
        "technical leadership", "staff", "principal", "tech lead",
        "mobile payments", "fido", "mfa", "samsung knox",
        "ci/cd", "jenkins", "github actions", "fastlane",
        "clean architecture", "mvi", "coroutines", "flow",
    )

    public val MODERATE_TECH_KEYWORDS: List<String> = listOf(
        "react native", "firebase", "gcp", "google cloud",
        "javascript", "typescript", "web development",
        "security", "authentication",
    )

    public val WEAK_TECH_KEYWORDS: List<String> = listOf(
        "swift", "objective-c", "ios", "embedded", "data engineering",
        "flutter", "dart",
    )

    public val EXPERIENCE_KEYWORDS: List<String> = listOf(
        "enterprise", "financial services", "fintech", "finserv",
        "banking", "payments", "mobile banking",
        "large scale", "scale", "3m+", "10m+", "millions",
        "modular", "multi-module", "build optimization",
        "technical debt", "refactor", "migration",
        "mentoring", "mentorship", "team lead", "architect",
        "cross-functional", "stakeholder",
    )

    public val BEHAVIORAL_KEYWORDS: List<String> = listOf(
        "best practice", "standard", "process", "governance",
        "documentation", "knowledge sharing",
        "autonomous", "independent", "ownership",
        "modernization", "transformation", "redesign",
        "developer experience", "devex", "tooling",
    )

    public val CAREER_KEYWORDS: List<String> = listOf(
        "staff", "principal", "lead architect", "mobile architect",
        "technical strategy", "technical direction",
        "fintech", "finserv", "financial services", "banking",
        "remote", "pittsburgh",
        "modernization", "platform",
    )

    // === Deal Breakers ===

    private val DEAL_BREAKERS: Map<String, List<String>> = mapOf(
        "consulting" to listOf(
            "consulting", "consultant", "consultancy", "management consulting",
            "staffing agency", "staff augmentation",
        ),
        "hybrid_stack" to listOf(
            "react native", "flutter", "hybrid mobile", "cross-platform framework",
        ),
        "people_management" to listOf(
            "people manager", "people management", "direct reports",
            "vp of engineering", "engineering manager", "director of engineering",
            "head of engineering", "cto", "cfo",
        ),
    )

    // === Core Scoring Functions ===

    /**
     * Score a dimension based on keyword matches in the job text.
     * Normalizes to 0-100, capped at keyword_count * 0.3 threshold.
     */
    public fun scoreDimension(text: String, keywords: List<String>): Int {
        if (keywords.isEmpty() || text.isBlank()) {
            return 0
        }
        val textLower = text.lowercase()
        val matches = keywords.count { kw -> kw.lowercase() in textLower }
        val score = (matches / maxOf(keywords.size * 0.3, 1.0)).coerceAtMost(1.0)
        return (score * 100).toInt()
    }

    /**
     * Check for deal-breaker patterns in the job text.
     * Returns a list of deal-breaker category names found.
     */
    public fun checkDealBreakers(text: String): List<String> {
        val textLower = text.lowercase()
        val found = mutableListOf<String>()
        for ((category, patterns) in DEAL_BREAKERS) {
            for (pattern in patterns) {
                if (pattern.lowercase() in textLower) {
                    found.add(category)
                    break
                }
            }
        }
        return found
    }

    /**
     * Try to extract company and role from job text.
     * Looks for "Role at Company" or "Company - Role" patterns in the first 10 lines.
     */
    public fun extractJobInfo(text: String): JobInfo {
        var company = ""
        var role = ""
        val lines = text.trim().split("\n").take(10)

        for (line in lines) {
            val stripped = line.trim()
            if (stripped.isEmpty()) continue

            if (" at " in stripped) {
                val parts = stripped.split(" at ", limit = 2)
                role = parts[0].trim()
                company = parts[1].trim()
                break
            } else if (" - " in stripped) {
                val parts = stripped.split(" - ", limit = 2)
                company = parts[0].trim()
                role = parts[1].trim()
                break
            }
        }
        return JobInfo(company, role)
    }

    // === Full Evaluation ===

    /**
     * Evaluate a job description and return scoring results.
     *
     * @param text The full job description text.
     * @return A [RankResult] with overall score, verdict, dimension scores, and analysis.
     */
    public fun evaluate(text: String): RankResult {
        val technicalScore = scoreDimension(text, STRONG_TECH_KEYWORDS)
        val experienceScore = scoreDimension(text, EXPERIENCE_KEYWORDS)
        val behavioralScore = scoreDimension(text, BEHAVIORAL_KEYWORDS)
        val careerScore = scoreDimension(text, CAREER_KEYWORDS)

        // Check for weak-match keywords that reduce the score
        val weakMatches = WEAK_TECH_KEYWORDS.filter { kw -> kw.lowercase() in text.lowercase() }
        val adjustedTechnical = maxOf(0, technicalScore - weakMatches.size * 10)

        // Weighted overall score
        var overall = (
            adjustedTechnical * WEIGHTS["technical"]!!
            + experienceScore * WEIGHTS["experience"]!!
            + behavioralScore * WEIGHTS["behavioral"]!!
            + careerScore * WEIGHTS["career"]!!
        ).toInt()

        // Verdict band
        val verdict = when {
            overall >= 85 -> "Strong Fit"
            overall >= 70 -> "Good Fit"
            overall >= 55 -> "Moderate Fit"
            overall >= 40 -> "Weak Fit"
            else -> "Poor Fit"
        }

        val dealBreakers = checkDealBreakers(text)
        val jobInfo = extractJobInfo(text)

        // Strengths and gaps
        val strengths = mutableListOf<String>()
        val gaps = mutableListOf<String>()

        if (adjustedTechnical >= 60) {
            strengths.add("Strong technical alignment with Android/Kotlin expertise")
        } else {
            gaps.add("Limited technical keyword match — may not be Android-focused")
        }

        if (experienceScore >= 60) {
            strengths.add("Experience requirements align with enterprise/mobile background")
        } else {
            gaps.add("Experience requirements may not match enterprise/mobile focus")
        }

        if (behavioralScore >= 60) {
            strengths.add("Cultural fit — aligns with process-driven, autonomous work style")
        } else {
            gaps.add("Behavioral fit unclear — may not emphasize autonomy or process improvement")
        }

        if (careerScore >= 60) {
            strengths.add("Career alignment — matches target role level and sector")
        } else {
            gaps.add("Career alignment weak — role level or sector may not match targets")
        }

        if (weakMatches.isNotEmpty()) {
            gaps.add("Weak-match technologies detected: ${weakMatches.joinToString(", ")}")
        }

        for (db in dealBreakers) {
            when (db) {
                "consulting" -> gaps.add("DEAL-BREAKER: appears to be a consulting role")
                "hybrid_stack" -> gaps.add("DEAL-BREAKER: primary focus on hybrid mobile stack (React Native/Flutter)")
                "people_management" -> gaps.add("DEAL-BREAKER: appears to be a people management role")
            }
        }

        return RankResult(
            overall = overall,
            verdict = verdict,
            technical = adjustedTechnical,
            experience = experienceScore,
            behavioral = behavioralScore,
            career = careerScore,
            deal_breakers = dealBreakers,
            strengths = strengths,
            gaps = gaps,
            company = jobInfo.company,
            role = jobInfo.role,
        )
    }

    // === Output Formatting ===

    /**
     * Format results as a markdown summary.
     */
    public fun formatMarkdown(result: RankResult, trackerRecord: Map<String, Any>?): String {
        val lines = mutableListOf<String>()
        lines.add("## Job Fit Evaluation")
        lines.add("")

        if (result.company.isNotBlank() && result.role.isNotBlank()) {
            lines.add("**${result.company} — ${result.role}**")
            lines.add("")
        }

        lines.add("**Overall Score:** ${result.overall}/100 — ${result.verdict}")
        lines.add("")

        lines.add("### Dimension Scores")
        lines.add("")
        lines.add("- **Technical (30%):** ${result.technical}")
        lines.add("- **Experience (25%):** ${result.experience}")
        lines.add("- **Behavioral (15%):** ${result.behavioral}")
        lines.add("- **Career (30%):** ${result.career}")
        lines.add("")

        if (result.strengths.isNotEmpty()) {
            lines.add("### Strengths")
            lines.add("")
            for (s in result.strengths) {
                lines.add("- $s")
            }
            lines.add("")
        }

        if (result.gaps.isNotEmpty()) {
            lines.add("### Gaps & Concerns")
            lines.add("")
            for (g in result.gaps) {
                lines.add("- $g")
            }
            lines.add("")
        }

        if (trackerRecord != null) {
            lines.add("### Tracker Status")
            lines.add("")
            val appId = trackerRecord["id"]?.toString() ?: "unknown"
            val appStatus = trackerRecord["status"]?.toString() ?: "unknown"
            lines.add("This job already exists in the tracker: `$appId` (Status: $appStatus)")
            lines.add("")
        }

        return lines.joinToString("\n")
    }

    /**
     * Format results as JSON.
     */
    public fun formatJson(result: RankResult, trackerRecord: Map<String, Any>?): String {
        val json = Json { prettyPrint = true; encodeDefaults = true }
        return json.encodeToString(result)
    }
}
