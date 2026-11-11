package dev.fnc314.tools.jobsearch.records.parser

import dev.fnc314.tools.jobsearch.parser.RankScorer
import dev.fnc314.tools.jobsearch.parser.RankResult
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
        // 3 matches out of 3 keywords; cap at 0.3 * 3 = 0.9 → 100%
        assertTrue(score > 0, "Should score positive for matching keywords")
        assertTrue(score <= 100, "Score should not exceed 100")
    }

    @Test
    fun testScoreDimensionCapsScore() {
        val text = "kotlin android gradle compose kmp mobile architecture devex staff principal"
        val keywords = listOf(
            "kotlin", "android", "gradle", "compose", "kmp",
            "mobile architecture", "devex", "staff", "principal"
        )
        val score = RankScorer.scoreDimension(text, keywords)
        assertTrue(score <= 100, "Score should be capped at 100")
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

    @Test
    fun testCheckDealBreakersReturnsEmptyForCleanPosting() {
        val text = "Senior Android Engineer to build native mobile applications in Kotlin"
        val breakers = RankScorer.checkDealBreakers(text)
        assertTrue(breakers.isEmpty(), "Should have no deal-breakers for clean posting")
    }

    // === Job Info Extraction ===

    @Test
    fun testExtractJobInfoFromAtPattern() {
        val text = "Senior Android Engineer at Capital One\n\nDescription here..."
        val info = RankScorer.extractJobInfo(text)
        assertEquals("Senior Android Engineer", info.role)
        assertEquals("Capital One", info.company)
    }

    @Test
    fun testExtractJobInfoFromDashPattern() {
        val text = "Capital One - Senior Android Engineer\n\nDescription here..."
        val info = RankScorer.extractJobInfo(text)
        assertEquals("Capital One", info.company)
        assertEquals("Senior Android Engineer", info.role)
    }

    @Test
    fun testExtractJobInfoReturnsEmptyForNoMatch() {
        val text = "Just some random text without company or role patterns"
        val info = RankScorer.extractJobInfo(text)
        assertEquals("", info.company)
        assertEquals("", info.role)
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
        // Deal-breakers and weak keywords should reduce the score
        assertTrue(result.overall < 70, "Deal-breaker role should score below 70")
    }

    @Test
    fun testEvaluateVerdictBands() {
        // Strong Fit posting
        val strongText = """
            Principal Android Architect at FinTech Corp
            Kotlin, Jetpack Compose, KMP, Gradle, mobile architecture, technical leadership.
            Enterprise financial services, mentoring, autonomous work, best practices.
            Remote or Pittsburgh based.
        """.trimIndent()
        val strong = RankScorer.evaluate(strongText)

        // Poor Fit posting
        val poorText = "iOS Swift developer for small startup"
        val poor = RankScorer.evaluate(poorText)

        assertTrue(
            strong.overall >= poor.overall,
            "Strong fit posting should score higher than poor fit"
        )
    }

    @Test
    fun testEvaluateWeakTechKeywordsReduceScore() {
        val text = """
            iOS Developer position requiring Swift and Objective-C experience.
            Some embedded systems knowledge preferred.
        """.trimIndent()

        val result = RankScorer.evaluate(text)
        // Weak keywords (Swift, Objective-C, iOS, embedded) should reduce technical score
        assertTrue(result.gaps.any { it.contains("Weak-match") }, "Should note weak-match technologies")
    }

    // === Markdown Formatting ===

    @Test
    fun testFormatMarkdownIncludesScore() {
        val result = RankScorer.evaluate("Android Engineer at Google")
        val markdown = RankScorer.formatMarkdown(result, trackerRecord = null)
        assertTrue(markdown.contains("Overall Score"), "Markdown should contain overall score")
        assertTrue(markdown.contains("Job Fit Evaluation"), "Markdown should contain title")
    }

    @Test
    fun testFormatMarkdownIncludesTrackerStatus() {
        val result = RankScorer.evaluate("Android Engineer at Google")
        val trackerRecord = mapOf("id" to "abc-123", "status" to "APPLIED")
        val markdown = RankScorer.formatMarkdown(result, trackerRecord)
        assertTrue(markdown.contains("abc-123"), "Markdown should contain tracker ID")
        assertTrue(markdown.contains("APPLIED"), "Markdown should contain tracker status")
    }

    // === JSON Formatting ===

    @Test
    fun testFormatJsonProducesValidJson() {
        val result = RankScorer.evaluate("Android Engineer at Google")
        val json = RankScorer.formatJson(result, trackerRecord = null)
        // Basic validation: starts with { and contains expected keys
        assertTrue(json.trim().startsWith("{"), "JSON should start with {")
        assertTrue(json.contains("\"overall\""), "JSON should contain overall key")
        assertTrue(json.contains("\"verdict\""), "JSON should contain verdict key")
    }
}
