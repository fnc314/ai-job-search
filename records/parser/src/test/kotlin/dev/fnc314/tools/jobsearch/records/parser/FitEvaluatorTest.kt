package dev.fnc314.tools.jobsearch.records.parser

import dev.fnc314.tools.jobsearch.parser.FitEvaluator
import dev.fnc314.tools.jobsearch.parser.FitResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FitEvaluatorTest {

    @Test
    fun testCapitalOneMobileEngineer4ExplicitCalibration() {
        val app = mapOf(
            "company" to "Capital One",
            "role" to "Mobile Engineer 4, Android",
            "location" to mapOf("city" to "Remote"),
            "jobUrl" to "https://capitalone.wd12.myworkdayjobs.com/example",
        )
        val result = FitEvaluator.evaluateFit(app)
        assertEquals(95, result.score)
        assertTrue(result.recommendation.contains("Strong Fit"))
    }

    @Test
    fun testPrincipalStaffAndroidRoleScoresHigh() {
        val app = mapOf(
            "company" to "Stripe",
            "role" to "Staff Android Engineer",
            "location" to mapOf("city" to "Remote"),
        )
        val result = FitEvaluator.evaluateFit(app)
        assertTrue(result.score >= 90, "Expected >= 90, got ${result.score}")
    }

    @Test
    fun testDealBreakersPenalizeScore() {
        val hybridApp = mapOf(
            "company" to "SomeStartup",
            "role" to "React Native Mobile Developer",
            "location" to mapOf("city" to "Remote"),
        )
        val hybridResult = FitEvaluator.evaluateFit(hybridApp)
        assertTrue(hybridResult.score < 70, "React Native deal-breaker should score < 70, got ${hybridResult.score}")

        val consultingApp = mapOf(
            "company" to "BigConsultingFirm",
            "role" to "Android Consultant",
            "location" to mapOf("city" to "Remote"),
        )
        val consultingResult = FitEvaluator.evaluateFit(consultingApp)
        assertTrue(consultingResult.score < 70, "Consulting deal-breaker should score < 70, got ${consultingResult.score}")
    }

    @Test
    fun testRedditCalibrationCap() {
        val app = mapOf(
            "company" to "Reddit",
            "role" to "Staff Android Engineer",
            "location" to mapOf("city" to "Remote"),
        )
        val result = FitEvaluator.evaluateFit(app)
        assertTrue(result.score <= 75, "Reddit calibration should cap at 75, got ${result.score}")
    }

    @Test
    fun testSeniorAndroidRoleScoresWell() {
        val app = mapOf(
            "company" to "DoorDash",
            "role" to "Senior Android Engineer",
            "location" to mapOf("city" to "Pittsburgh", "region" to "PA"),
        )
        val result = FitEvaluator.evaluateFit(app)
        assertTrue(result.score >= 80, "Senior Android in Pittsburgh should score >= 80, got ${result.score}")
    }

    @Test
    fun testFinServDomainBonus() {
        val app = mapOf(
            "company" to "Chase",
            "role" to "Principal Android Engineer",
            "location" to mapOf("city" to "New York"),
        )
        val result = FitEvaluator.evaluateFit(app)
        assertTrue(result.behavioralMatch.any { it.contains("FinTech") || it.contains("FinServ") })
    }

    @Test
    fun testRemoteLocationBonus() {
        val app = mapOf(
            "company" to "Acme",
            "role" to "Senior Android Engineer",
            "location" to mapOf("city" to "Remote"),
        )
        val result = FitEvaluator.evaluateFit(app)
        assertTrue(result.behavioralMatch.any { it.contains("Remote") || it.contains("Pittsburgh") })
    }

    @Test
    fun testScoreBounds() {
        // Even with all penalties, score should not go below 0
        val app = mapOf(
            "company" to "Consulting Corp",
            "role" to "React Native Consultant",
            "location" to mapOf("city" to "Remote"),
        )
        val result = FitEvaluator.evaluateFit(app)
        assertTrue(result.score in 0..100, "Score should be in [0, 100], got ${result.score}")
    }

    @Test
    fun testRecommendationThresholds() {
        val strongApp = mapOf(
            "company" to "Capital One",
            "role" to "Mobile Engineer 4, Android",
            "location" to mapOf("city" to "Remote"),
        )
        val strongResult = FitEvaluator.evaluateFit(strongApp)
        assertTrue(strongResult.recommendation.contains("Strong Fit"))

        val moderateApp = mapOf(
            "company" to "Acme",
            "role" to "Android Developer",
            "location" to mapOf("city" to "Remote"),
        )
        val moderateResult = FitEvaluator.evaluateFit(moderateApp)
        assertTrue(moderateResult.recommendation.contains("Moderate Fit") || moderateResult.recommendation.contains("Strong Fit"))
    }

    @Test
    fun testEmptyApplicationReturnsBaseline() {
        val app = emptyMap<String, Any>()
        val result = FitEvaluator.evaluateFit(app)
        assertTrue(result.score in 0..100)
        assertTrue(result.skillsMatch.isNotEmpty())
        assertTrue(result.experienceMatch.isNotEmpty())
        assertTrue(result.behavioralMatch.isNotEmpty())
    }
}
