package dev.fnc314.tools.jobtracker.ranking

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ScoringWeightsTest {

    @Test
    fun testDefaultWeightsSumToOne() {
        val weights = ScoringWeights.DEFAULT
        val sum = weights.technical + weights.experience + weights.behavioral + weights.career
        assertEquals(1.0, sum, 0.01, "DEFAULT weights must sum to 1.0")
    }

    @Test
    fun testDefaultWeightsValidate() {
        ScoringWeights.DEFAULT.validate() // Should not throw
    }

    @Test
    fun testInvalidWeightsSumThrows() {
        val weights = ScoringWeights(
            technical = 0.50,
            experience = 0.50,
            behavioral = 0.50,
            career = 0.50,
        )
        assertFailsWith<IllegalArgumentException> {
            weights.validate()
        }
    }

    @Test
    fun testNegativeWeightsThrow() {
        val weights = ScoringWeights(
            technical = -0.10,
            experience = 0.35,
            behavioral = 0.15,
            career = 0.60,
        )
        assertFailsWith<IllegalArgumentException> {
            weights.validate()
        }
    }

    @Test
    fun testNegativeMultiplierThrows() {
        val weights = ScoringWeights(
            roleLevelMultiplier = -1.0,
        )
        assertFailsWith<IllegalArgumentException> {
            weights.validate()
        }
    }

    @Test
    fun testCustomValidWeights() {
        val weights = ScoringWeights(
            technical = 0.40,
            experience = 0.20,
            behavioral = 0.10,
            career = 0.30,
        )
        weights.validate() // Should not throw
    }

    @Test
    fun testZeroWeightDimensionAllowed() {
        val weights = ScoringWeights(
            technical = 0.25,
            experience = 0.25,
            behavioral = 0.50,
            career = 0.0,
        )
        weights.validate() // Should not throw — zero weight is allowed
    }

    @Test
    fun testSerializationRoundTrip() {
        val weights = ScoringWeights(
            technical = 0.40,
            experience = 0.20,
            behavioral = 0.10,
            career = 0.30,
            roleLevelMultiplier = 1.5,
            sectorMultiplier = 0.8,
            locationMultiplier = 1.2,
        )
        val json = Json { encodeDefaults = true }
        val encoded = json.encodeToString(weights)
        val decoded = json.decodeFromString<ScoringWeights>(encoded)
        assertEquals(weights, decoded, "Serialization round-trip should preserve all values")
    }
}
