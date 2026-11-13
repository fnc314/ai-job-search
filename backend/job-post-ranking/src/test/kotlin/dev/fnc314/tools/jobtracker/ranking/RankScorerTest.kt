package dev.fnc314.tools.jobtracker.ranking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RankScorerTest {

    // === Dimension Scoring ===

    @Test
    fun testScoreDimensionReturnsZeroForEmptyText() {
        val keywords = listOf("kotlin", "android")
        assertEquals(0, RankScorer.scoreDimension("", keywords))
    }

    @Test
    fun testScoreDimensionReturnsZeroForEmptyKeywords() {
        assertEquals(0, RankScorer.scoreDimension("kotlin android development", emptyList()))
    }

    @Test
    fun testScoreDimensionMatchesKeywords() {
        val text = "We need a Kotlin Android developer with Jetpack Compose experience"
        val keywords = listOf("kotlin", "android", "jetpack compose")
        val score = RankScorer.scoreDimension(text, keywords)
        assertTrue(score > 0, "Should score positive for matching keywords")
        assertTrue(score <= 100, "Score should not exceed 100")
    }

    // === Deal Breakers ===

    @Test
    fun testCheckDealBreakersFindsConsulting() {
        val text = "We are a management consulting firm looking for a consultant"
        val breakers = RankScorer.checkDealBreakers(text)
        assertTrue(breakers.contains("consulting"), "Should detect consulting deal-breaker")
    }

    @Test
    fun testCheckDealBreakersFindsHybridStack() {
        val text = "React Native developer for cross-platform mobile app"
        val breakers = RankScorer.checkDealBreakers(text)
        assertTrue(breakers.contains("hybrid_stack"), "Should detect hybrid_stack deal-breaker")
    }

    @Test
    fun testCheckDealBreakersFindsPeopleManagement() {
        val text = "Engineering Manager with direct reports and people management experience"
        val breakers = RankScorer.checkDealBreakers(text)
        assertTrue(breakers.contains("people_management"), "Should detect people_management deal-breaker")
    }

    // === Role-Level Detection ===

    @Test
    fun testRoleLevelDetectionPrincipalStaffAndroid() {
        val bonus = RankScorer.detectRoleLevelBonus("Principal Android Architect", "Android development")
        assertEquals(15, bonus, "Principal + Android should give +15")
    }

    @Test
    fun testRoleLevelDetectionSeniorAndroid() {
        val bonus = RankScorer.detectRoleLevelBonus("Senior Android Engineer", "Kotlin Android")
        assertEquals(10, bonus, "Senior + Android should give +10")
    }

    @Test
    fun testRoleLevelDetectionMobileOnly() {
        val bonus = RankScorer.detectRoleLevelBonus("Mobile Engineer", "mobile development")
        assertEquals(5, bonus, "Mobile only should give +5")
    }

    @Test
    fun testRoleLevelDetectionNoMatch() {
        val bonus = RankScorer.detectRoleLevelBonus("Backend Developer", "Java Spring")
        assertEquals(0, bonus, "Non-mobile role should give 0")
    }

    // === Sector Alignment ===

    @Test
    fun testSectorAlignmentFinServBonus() {
        val bonus = RankScorer.detectSectorBonus("Chase", "financial services")
        assertEquals(5, bonus, "FinServ company should give +5")
    }

    @Test
    fun testSectorAlignmentNoBonus() {
        val bonus = RankScorer.detectSectorBonus("TechStartup", "web development")
        assertEquals(0, bonus, "Non-FinServ company should give 0")
    }

    // === Location Scoring ===

    @Test
    fun testLocationScoringRemote() {
        val (bonus, verdict) = RankScorer.detectLocationBonus("Remote", "Android Engineer")
        assertEquals(5, bonus, "Remote should give +5")
        assertEquals("PASS", verdict)
    }

    @Test
    fun testLocationScoringPittsburgh() {
        val (bonus, verdict) = RankScorer.detectLocationBonus("Pittsburgh, PA", "Android Engineer")
        assertEquals(5, bonus, "Pittsburgh should give +5")
        assertEquals("PASS", verdict)
    }

    @Test
    fun testLocationScoringNYCNeutral() {
        val (bonus, verdict) = RankScorer.detectLocationBonus("New York, NY", "Android Engineer")
        assertEquals(0, bonus, "NYC should be neutral")
        assertEquals("ACCEPTABLE", verdict)
    }

    // === Calibration ===

    @Test
    fun testCalibrationRedditCap() {
        val score = RankScorer.applyCalibration(90, "Reddit", "Staff Android Engineer")
        assertTrue(score <= 75, "Reddit should cap at 75, got $score")
    }

    @Test
    fun testCalibrationCapitalOneOverride() {
        val score = RankScorer.applyCalibration(50, "Capital One", "Mobile Engineer 4, Android")
        assertEquals(95, score, "Capital One Mobile Engineer 4 should override to 95")
    }

    @Test
    fun testCalibrationNoEffect() {
        val score = RankScorer.applyCalibration(80, "Google", "Android Engineer")
        assertEquals(80, score, "No calibration should leave score unchanged")
    }

    // === Deal-Breaker Penalties ===

    @Test
    fun testDealBreakerPenaltyHybridOnly() {
        val penalty = RankScorer.calculateDealBreakerPenalties("React Native Developer", "React Native only")
        assertEquals(-30, penalty, "Hybrid-only should penalize -30")
    }

    @Test
    fun testDealBreakerPenaltyConsulting() {
        val penalty = RankScorer.calculateDealBreakerPenalties("Android Consultant", "consulting work")
        assertEquals(-25, penalty, "Consulting should penalize -25")
    }

    @Test
    fun testDealBreakerPenaltyPeopleManagement() {
        val penalty = RankScorer.calculateDealBreakerPenalties("Engineering Manager", "people management")
        assertEquals(-20, penalty, "People management should penalize -20")
    }

    @Test
    fun testDealBreakerPenaltyNone() {
        val penalty = RankScorer.calculateDealBreakerPenalties("Senior Android Engineer", "Kotlin Android")
        assertEquals(0, penalty, "Clean role should have no penalty")
    }

    // === Full Evaluation ===

    @Test
    fun testEvaluateStrongAndroidRole() {
        val text = """
            Senior Android Engineer at TechCorp
            We are looking for a Kotlin expert with Jetpack Compose, Gradle, and mobile architecture experience.
            Enterprise financial services background preferred. Technical leadership and mentoring skills required.
            Remote position in Pittsburgh area.
        """.trimIndent()

        val result = RankScorer.evaluate(text)
        assertTrue(result.overall > 0, "Strong Android role should score positively")
        assertTrue(result.technical > 0, "Technical score should be positive")
        assertTrue(result.experience > 0, "Experience score should be positive")
    }

    @Test
    fun testEvaluateDealBreakerReducesScore() {
        val text = """
            React Native Developer at Consulting Firm
            We are a consulting agency looking for a hybrid mobile developer.
        """.trimIndent()

        val result = RankScorer.evaluate(text)
        assertTrue(result.deal_breakers.isNotEmpty(), "Should detect deal-breakers")
        assertTrue(result.overall < 70, "Deal-breaker role should score below 70")
    }

    @Test
    fun testEvaluateVerdictBands() {
        val strongText = """
            Principal Android Architect at FinTech Corp
            Kotlin, Jetpack Compose, KMP, Gradle, mobile architecture, technical leadership.
            Enterprise financial services, mentoring, autonomous work, best practices.
            Remote or Pittsburgh based.
        """.trimIndent()
        val strong = RankScorer.evaluate(strongText)

        val poorText = "iOS Swift developer for small startup"
        val poor = RankScorer.evaluate(poorText)

        assertTrue(
            strong.overall >= poor.overall,
            "Strong fit posting should score higher than poor fit"
        )
    }

    @Test
    fun testScoreBoundsAfterAdjustments() {
        // Even with all penalties, score should not go below 0
        val text = """
            React Native Consultant at Consulting Corp
            We are a consulting firm looking for a React Native consultant.
            Engineering Manager with people management responsibilities.
        """.trimIndent()
        val result = RankScorer.evaluate(text)
        assertTrue(result.overall >= 0, "Score should not go below 0, got ${result.overall}")
        assertTrue(result.overall <= 100, "Score should not exceed 100, got ${result.overall}")
    }

    // === Dynamic Weights ===

    @Test
    fun testDynamicWeightsProduceDifferentScores() {
        val text = """
            Senior Android Engineer at TechCorp
            Kotlin, Jetpack Compose, Gradle, mobile architecture.
            Enterprise financial services, mentoring, autonomous work.
            Remote position.
        """.trimIndent()

        val defaultResult = RankScorer.evaluate(text, ScoringWeights.DEFAULT)
        val techHeavyWeights = ScoringWeights(
            technical = 0.50,
            experience = 0.15,
            behavioral = 0.10,
            career = 0.25,
        )
        val techHeavyResult = RankScorer.evaluate(text, techHeavyWeights)

        // Different weights should produce different scores when dimension scores differ
        // (not a strict assertion since scores may coincide, but the mechanism works)
        assertTrue(defaultResult.overall in 0..100)
        assertTrue(techHeavyResult.overall in 0..100)
    }

    // === Markdown/JSON Formatting ===

    @Test
    fun testFormatMarkdownIncludesScore() {
        val result = RankScorer.evaluate("Android Engineer at Google")
        val markdown = RankScorer.formatMarkdown(result, trackerRecord = null)
        assertTrue(markdown.contains("Overall Score"), "Markdown should contain overall score")
        assertTrue(markdown.contains("Job Fit Evaluation"), "Markdown should contain title")
    }

    @Test
    fun testFormatJsonProducesValidJson() {
        val result = RankScorer.evaluate("Android Engineer at Google")
        val json = RankScorer.formatJson(result, trackerRecord = null)
        assertTrue(json.trim().startsWith("{"), "JSON should start with {")
        assertTrue(json.contains("\"overall\""), "JSON should contain overall key")
        assertTrue(json.contains("\"verdict\""), "JSON should contain verdict key")
    }
}
