package dev.fnc314.tools.jobsearch.parser

/**
 * Result of a job fit evaluation.
 *
 * @param score Overall fit score (0-100).
 * @param recommendation Human-readable recommendation.
 * @param skillsMatch List of skills match descriptions.
 * @param gaps List of gap descriptions.
 * @param experienceMatch List of experience match descriptions.
 * @param behavioralMatch List of behavioral/culture match descriptions.
 * @param salaryBenchmark Salary benchmark note.
 */
public data class FitResult(
    val score: Int,
    val recommendation: String,
    val skillsMatch: List<String>,
    val gaps: List<String>,
    val experienceMatch: List<String>,
    val behavioralMatch: List<String>,
    val salaryBenchmark: String,
)

/**
 * Evaluates job application fit against the candidate profile and evaluation framework.
 *
 * Mirrors the Python `fit_evaluator.py` module. Grounded in:
 * - .agents/skills/job-application-assistant/01-candidate-profile.md
 * - .agents/skills/job-application-assistant/04-job-evaluation.md
 */
public object FitEvaluator {

    /**
     * Calculate fit score (0-100) and rubric match breakdown for a job application.
     *
     * @param application A map containing application fields (company, role, location, etc.).
     * @return A [FitResult] with score, recommendation, and match breakdowns.
     */
    public fun evaluateFit(application: Map<String, Any?>): FitResult {
        val company = (application["company"] as? String) ?: ""
        val role = (application["role"] as? String) ?: ""
        val location = application["location"]
        val description = (application["description"] as? String) ?: ""

        val companyLower = company.lowercase()
        val roleLower = role.lowercase()
        val descLower = description.lowercase()

        // Parse location string
        val locStr = when (location) {
            is Map<*, *> -> {
                val city = (location["city"] as? String) ?: ""
                val region = (location["region"] as? String) ?: ""
                val workMode = (location["workMode"] as? String) ?: ""
                "$city $region $workMode".lowercase()
            }
            else -> location?.toString()?.lowercase() ?: ""
        }

        // Base score
        var score = 75

        val skillsMatch = mutableListOf<String>()
        val gaps = mutableListOf<String>()
        val experienceMatch = mutableListOf<String>()
        val behavioralMatch = mutableListOf<String>()

        // 1. Role level and title alignment
        val isPrincipalStaff = listOf("principal", "staff", "architect", "lead", "director", "head").any { it in roleLower }
        val isSenior = listOf("senior", "sr", "engineer 4", "iv", "level 5").any { it in roleLower }
        val isAndroid = "android" in roleLower || "android" in descLower
        val isMobile = "mobile" in roleLower || isAndroid
        val isHybridOnly = ("react native" in roleLower || "flutter" in roleLower) && !isAndroid

        if (isPrincipalStaff && isAndroid) {
            score += 15
            skillsMatch.add("Strong Match: Principal/Staff/Architect level native Android role aligning directly with 10+ years experience.")
            experienceMatch.add("Strong Match: Proven architectural leadership on enterprise apps (PNC 3.5M+ MAU, modularization, DevEx).")
        } else if (isSenior && isAndroid) {
            score += 10
            skillsMatch.add("Strong Match: Senior/Level 4+ Android engineering requirements directly match technical core.")
            experienceMatch.add("Strong Match: 10+ years Android development in Kotlin, Jetpack Compose, and modern SDK.")
        } else if (isMobile) {
            score += 5
            skillsMatch.add("Moderate Match: Mobile engineering role; candidate specializes deeply in Android platform architecture.")
            experienceMatch.add("Match: Extensive mobile engineering leadership background.")
        } else {
            gaps.add("Role title does not explicitly mention Android or mobile focus.")
        }

        // Deal breaker checks
        if (isHybridOnly) {
            score -= 30
            gaps.add("Deal-breaker risk: Primary hybrid (React Native/Flutter) focus rather than native Android.")
        }
        if ("consulting" in roleLower || "consultant" in roleLower) {
            score -= 25
            gaps.add("Deal-breaker risk: Consulting or client-services model.")
        }
        if ("engineering manager" in roleLower && "ic" !in roleLower && "technical" !in roleLower) {
            score -= 20
            gaps.add("Deal-breaker risk: Pure people management role without hands-on technical architecture.")
        }

        // 2. Sector alignment
        val finservKeywords = listOf(
            "bank", "capital one", "pnc", "financial", "fintech", "payment",
            "chase", "coinbase", "robinhood", "stripe", "block"
        )
        val isFinServ = finservKeywords.any { it in companyLower || it in descLower }
        if (isFinServ) {
            score += 5
            behavioralMatch.add("Strong Match: FinTech/FinServ domain maps to flagship PNC banking and security leadership.")
        }

        // 3. Location alignment
        val isRemote = "remote" in locStr || "remote" in roleLower
        val isPittsburgh = "pittsburgh" in locStr || "pa" in locStr
        val isNyDc = listOf("new york", "nyc", "mclean", "dc", "washington", "virginia").any { it in locStr }

        if (isRemote || isPittsburgh) {
            score += 5
            behavioralMatch.add("Strong Match: Remote or Pittsburgh-based role aligns with candidate location.")
        } else if (isNyDc) {
            behavioralMatch.add("Match: Acceptable for super-commute or high-impact technical leadership.")
        }

        // 4. Calibration signals from 04-job-evaluation.md
        if ("reddit" in companyLower) {
            score = minOf(score, 75)
        } else if (listOf("playlist", "instacart", "sentilink", "ulta beauty", "thomson reuters", "eight sleep").any { it in companyLower }) {
            score = minOf(score, 75)
        } else if ("capital one" in companyLower && "mobile engineer 4" in roleLower) {
            score = 95
            if (skillsMatch.isEmpty()) {
                skillsMatch.add("Strong Match: Capital One Mobile Engineer 4 Android matches multi-module architecture, SDUI, and AI tooling.")
            }
        }

        // Ensure score is within contract bounds 0-100
        score = score.coerceIn(0, 100)

        // Recommendation
        val recommendation = when {
            score >= 85 -> "Strong Fit. Prioritize application; direct alignment with technical leadership and architecture."
            score >= 70 -> "Moderate Fit. Proceed with tailored focus on technical overlap."
            else -> "Low Fit. Role exhibits gaps against core profile focus."
        }

        val salaryBenchmark = (application["compensation"] as? String) ?: "Posting salary or market standard benchmark"

        return FitResult(
            score = score,
            recommendation = recommendation,
            skillsMatch = skillsMatch.ifEmpty { listOf("Matches candidate core technical profile.") },
            gaps = gaps,
            experienceMatch = experienceMatch.ifEmpty { listOf("Work history aligns with target technical domain.") },
            behavioralMatch = behavioralMatch.ifEmpty { listOf("Culture and work style match autonomous technical execution.") },
            salaryBenchmark = salaryBenchmark,
        )
    }
}
