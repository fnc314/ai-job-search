package dev.fnc314.tools.jobtracker.ranking

import kotlinx.serialization.json.Json

/**
 * Unified deterministic job fit scorer.
 *
 * Combines keyword-based dimension scoring with role-level detection,
 * sector alignment, location scoring, calibration rules, and deal-breaker penalties.
 * Absorbs all rules from the former FitEvaluator.
 *
 * Scoring pipeline:
 * 1. Keyword dimension scoring (Technical/Experience/Behavioral/Career)
 * 2. Role-level detection bonuses
 * 3. Sector alignment bonuses
 * 4. Location scoring bonuses
 * 5. Calibration caps and overrides
 * 6. Deal-breaker penalties
 * 7. Verdict band assignment
 */
public object RankScorer {

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
            // Strict keywords only — terms that explicitly imply managing people.
            // "Thought leader", "technical anchor", "drives integration projects" are NOT
            // people management — they describe technical influence and IC leadership.
            "people manager", "people management", "direct reports", "direct-reports",
            "managerial responsibility", "managerial responsibilities", "managerial role",
            "vp of engineering", "engineering manager", "director of engineering",
            "head of engineering", "cto", "cfo",
        ),
    )

    // === Sector Keywords ===

    private val FINSERV_KEYWORDS: List<String> = listOf(
        "bank", "capital one", "pnc", "financial", "fintech", "payment",
        "chase", "coinbase", "robinhood", "stripe", "block",
    )

    // === Role Level Keywords ===

    private val PRINCIPAL_STAFF_KEYWORDS: List<String> = listOf(
        "principal", "staff", "architect", "lead", "director", "head",
    )
    private val SENIOR_KEYWORDS: List<String> = listOf(
        "senior", "sr", "engineer 4", "iv", "level 5",
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
     * Try to extract company, role, and location from job text.
     * Looks for "Role at Company" or "Company - Role" patterns in the first 10 lines.
     */
    public fun extractJobInfo(text: String): JobInfo {
        var company = ""
        var role = ""
        var location = ""
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
        return JobInfo(company, role, location)
    }

    // === Role-Level Detection (from FitEvaluator) ===

    /**
     * Detect role level and return a score bonus.
     * +15 for principal/staff + Android, +10 for senior + Android, +5 for mobile only.
     */
    public fun detectRoleLevelBonus(role: String, description: String): Int {
        val roleLower = role.lowercase()
        val descLower = description.lowercase()
        val isPrincipalStaff = PRINCIPAL_STAFF_KEYWORDS.any { it in roleLower }
        val isSenior = SENIOR_KEYWORDS.any { it in roleLower }
        val isAndroid = "android" in roleLower || "android" in descLower
        val isMobile = "mobile" in roleLower || isAndroid

        return when {
            isPrincipalStaff && isAndroid -> 15
            isSenior && isAndroid -> 10
            isMobile -> 5
            else -> 0
        }
    }

    // === Sector Alignment (from FitEvaluator) ===

    /**
     * Check sector alignment and return a score bonus.
     * +5 for FinTech/FinServ keywords in company name or description.
     */
    public fun detectSectorBonus(company: String, description: String): Int {
        val companyLower = company.lowercase()
        val descLower = description.lowercase()
        return if (FINSERV_KEYWORDS.any { it in companyLower || it in descLower }) {
            5
        } else {
            0
        }
    }

    // === Location Scoring (from FitEvaluator) ===

    /**
     * Evaluate location and return a score bonus plus verdict.
     * +5 for remote/Pittsburgh, neutral for NYC/DC.
     * Returns a pair of (bonus, verdict).
     */
    public fun detectLocationBonus(location: String, role: String): Pair<Int, String> {
        val locStr = location.lowercase()
        val roleLower = role.lowercase()
        val isRemote = "remote" in locStr || "remote" in roleLower
        val isPittsburgh = "pittsburgh" in locStr || "pa" in locStr
        val isNyDc = listOf("new york", "nyc", "mclean", "dc", "washington", "virginia")
            .any { it in locStr }

        return when {
            isRemote || isPittsburgh -> 5 to "PASS"
            isNyDc -> 0 to "ACCEPTABLE"
            else -> 0 to "NEUTRAL"
        }
    }

    // === Calibration (from FitEvaluator) ===

    /**
     * Apply calibration rules to a score.
     * Override takes precedence over cap.
     */
    public fun applyCalibration(score: Int, company: String, role: String): Int {
        val companyLower = company.lowercase()
        val roleLower = role.lowercase()

        // Capital One Mobile Engineer 4 override
        if ("capital one" in companyLower && "mobile engineer 4" in roleLower) {
            return 95
        }

        // Check calibration rules by company slug
        val slug = companyLower
            .replace(Regex("(inc|llc|corp|company|ltd|the|of|and)\\b"), "")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        val rule = CALIBRATION_RULES[slug]
        if (rule != null) {
            return when {
                rule.overrideScore != null -> rule.overrideScore
                rule.maxScore != null -> minOf(score, rule.maxScore)
                else -> score
            }
        }

        // Reddit and known low-conversion companies cap
        if ("reddit" in companyLower) {
            return minOf(score, 75)
        }
        if (listOf("playlist", "instacart", "sentilink", "ulta beauty", "thomson reuters", "eight sleep")
            .any { it in companyLower }
        ) {
            return minOf(score, 75)
        }

        return score
    }

    // === Deal-Breaker Penalties (from FitEvaluator) ===

    /**
     * Apply deal-breaker penalties based on role and description.
     * Returns the total penalty amount.
     */
    public fun calculateDealBreakerPenalties(role: String, description: String): Int {
        val roleLower = role.lowercase()
        val descLower = description.lowercase()
        var penalty = 0

        // Hybrid-only stack: React Native or Flutter without Android
        val isAndroid = "android" in roleLower || "android" in descLower
        val isHybridOnly = ("react native" in roleLower || "flutter" in roleLower) && !isAndroid
        if (isHybridOnly) {
            penalty -= 30
        }

        // Consulting
        if ("consulting" in roleLower || "consultant" in roleLower) {
            penalty -= 25
        }

        // Pure people management
        if ("engineering manager" in roleLower && "ic" !in roleLower && "technical" !in roleLower) {
            penalty -= 20
        }

        return penalty
    }

    // === Full Evaluation ===

    /**
     * Evaluate a job description and return scoring results.
     *
     * @param text The full job description text.
     * @param weights Dynamic scoring weights configuration.
     * @return A [RankResult] with overall score, verdict, dimension scores, and analysis.
     */
    public fun evaluate(text: String, weights: ScoringWeights = ScoringWeights.DEFAULT): RankResult {
        weights.validate()

        // 1. Keyword dimension scoring
        val technicalScore = scoreDimension(text, STRONG_TECH_KEYWORDS)
        val experienceScore = scoreDimension(text, EXPERIENCE_KEYWORDS)
        val behavioralScore = scoreDimension(text, BEHAVIORAL_KEYWORDS)
        val careerScore = scoreDimension(text, CAREER_KEYWORDS)

        // Weak-match keywords reduce the technical score
        val weakMatches = WEAK_TECH_KEYWORDS.filter { kw -> kw.lowercase() in text.lowercase() }
        val adjustedTechnical = maxOf(0, technicalScore - weakMatches.size * 10)

        // 2. Weighted overall score using dynamic weights
        var overall = (
            adjustedTechnical * weights.technical
            + experienceScore * weights.experience
            + behavioralScore * weights.behavioral
            + careerScore * weights.career
        ).toInt()

        // 3. Role-level detection bonus
        val jobInfo = extractJobInfo(text)
        val roleBonus = detectRoleLevelBonus(jobInfo.role, text)
        overall += (roleBonus * weights.roleLevelMultiplier).toInt()

        // 4. Sector alignment bonus
        val sectorBonus = detectSectorBonus(jobInfo.company, text)
        overall += (sectorBonus * weights.sectorMultiplier).toInt()

        // 5. Location scoring bonus
        val (locationBonus, locationVerdict) = detectLocationBonus(jobInfo.location, jobInfo.role)
        overall += (locationBonus * weights.locationMultiplier).toInt()

        // 6. Calibration caps and overrides
        overall = applyCalibration(overall, jobInfo.company, jobInfo.role)

        // 7. Deal-breaker penalties
        val dealBreakers = checkDealBreakers(text)
        val dealBreakerPenalty = calculateDealBreakerPenalties(jobInfo.role, text)
        overall += dealBreakerPenalty

        // Clamp to 0-100
        overall = overall.coerceIn(0, 100)

        // Verdict band
        val verdict = when {
            overall >= 85 -> "Strong Fit"
            overall >= 70 -> "Good Fit"
            overall >= 55 -> "Moderate Fit"
            overall >= 40 -> "Weak Fit"
            else -> "Poor Fit"
        }

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
            locationVerdict = locationVerdict,
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
