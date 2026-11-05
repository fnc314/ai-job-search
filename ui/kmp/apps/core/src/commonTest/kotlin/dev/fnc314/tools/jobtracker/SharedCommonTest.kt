package dev.fnc314.tools.jobtracker

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SharedCommonTest {

    @Test
    fun testFitScoreFromFitRating() {
        val app = Factory.createJobApplication(
            id = "test-1",
            company = "Test Corp",
            fitRating = 85,
        )
        assertEquals(85, app.fitRating)
        assertEquals(85, app.fitScore)
        assertNotNull(app.fitEvaluation)
        assertEquals(85, app.fitEvaluation?.score)
    }

    @Test
    fun testFitScoreFromFitEvaluation() {
        val fit = Factory.createTrackerFit(score = 92)
        val app = Factory.createJobApplication(
            id = "test-2",
            company = "Test Corp 2",
            fitEvaluation = fit,
        )
        assertEquals(92, app.fitRating)
        assertEquals(92, app.fitScore)
        assertEquals(92, app.fitEvaluation?.score)
    }
}
