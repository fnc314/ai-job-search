package dev.fnc314.tools.jobtracker.core.data

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JobApplicationTest {

    @Test
    fun testFitScorePrecedence_FitRatingWins() {
        val fit = Factory.createTrackerFit(score = 95)
        val app = Factory.createJobApplication(
            id = "test-1",
            company = "Acme Corp",
            fitRating = 70,
            fitEvaluation = fit,
        )
        // fitRating scalar must strictly take precedence over fitEvaluation score
        assertEquals(70, app.fitRating)
        assertEquals(70, app.fitScore)
        assertEquals(95, app.fitEvaluation?.score)
    }

    @Test
    fun testFitScore_FallbackToFitEvaluationWhenRatingNull() {
        val fit = Factory.createTrackerFit(score = 88)
        val app = Factory.createJobApplication(
            id = "test-2",
            company = "Beta LLC",
            fitRating = null,
            fitEvaluation = fit,
        )
        assertEquals(88, app.fitRating)
        assertEquals(88, app.fitScore)
        assertEquals(88, app.fitEvaluation?.score)
    }

    @Test
    fun testFitScore_BothNullReturnsNull() {
        val app = Factory.createJobApplication(
            id = "test-3",
            company = "Gamma Inc",
            fitRating = null,
            fitEvaluation = null,
        )
        assertNull(app.fitRating)
        assertNull(app.fitScore)
        assertNull(app.fitEvaluation)
    }

    @Test
    fun testFitScore_ZeroValuePreserved() {
        val app = Factory.createJobApplication(
            id = "test-4",
            company = "Delta Co",
            fitRating = 0,
            fitEvaluation = null,
        )
        // 0 must not be coerced to null or treated as missing
        assertEquals(0, app.fitRating)
        assertEquals(0, app.fitScore)
        assertNotNull(app.fitEvaluation)
        assertEquals(0, app.fitEvaluation.score)
    }

    @Test
    fun testJobApplication_CopyPreservesImmutableCollections() {
        val app = Factory.createJobApplication(
            id = "test-5",
            company = "Epsilon Ltd",
            tags = listOf("kotlin", "remote"),
            contactIds = listOf("c1", "c2"),
        )
        val copied = app.copy(company = "Epsilon Renamed")
        assertEquals("Epsilon Renamed", copied.company)
        assertEquals(listOf("kotlin", "remote"), copied.tags)
        assertEquals(listOf("c1", "c2"), copied.contactIds)
        assertEquals(app.fitScore, copied.fitScore)
    }
}
